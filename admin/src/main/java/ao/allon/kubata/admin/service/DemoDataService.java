package ao.allon.kubata.admin.service;

import ao.allon.kubata.core.domain.Fornecedor;
import ao.allon.kubata.core.repository.FornecedorRepository;
import ao.allon.kubata.core.domain.Empresa;
import ao.allon.kubata.core.domain.Role;
import ao.allon.kubata.core.domain.TipoConta;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.repository.EmpresaRepository;
import ao.allon.kubata.core.repository.UserRepository;
import ao.allon.kubata.inventario.domain.Categoria;
import ao.allon.kubata.inventario.domain.Produto;
import ao.allon.kubata.inventario.enums.UnidadeMedida;
import ao.allon.kubata.inventario.repository.CategoriaRepository;
import ao.allon.kubata.inventario.repository.ProdutoRepository;
import ao.allon.kubata.vendas.domain.Cliente;
import ao.allon.kubata.vendas.enums.TipoCliente;
import ao.allon.kubata.vendas.repository.ClienteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Geração e remoção de dados de demonstração (ambiente de testes/visita).
 *
 * <p>No espirito do Primavera ERP — onde existe uma "empresa de demonstração"
 * pré-preenchida — o Kubata Administrator pode criar em segundos um ambiente
 * completo para demonstrar o sistema a um cliente: empresa fictícia com
 * exercício fiscal aberto, utilizadores de teste com perfis distintos
 * (administrador, vendedor, caixa, contabilista) e dados mestres de amostra
 * (categorias, artigos, clientes e fornecedores).</p>
 *
 * <p>Todos os registos criados são marcados de forma determinística
 * (prefixos "DEMO-" em códigos/NIFs, "Demo - " em nomes) para que possam ser
 * removidos de forma segura por {@link #limparDadosDemo(Consumer)} sem tocar
 * em dados reais de produção.</p>
 */
@Service
public class DemoDataService {

    private static final Logger logger = LoggerFactory.getLogger(DemoDataService.class);

    /** NIF distinto da empresa de demonstração (pessoa colectiva, 9 dígitos). */
    public static final String NIF_EMPRESA_DEMO = "5419999991";
    public static final String CODIGO_EMPRESA_DEMO = "DEM";
    public static final String NOME_EMPRESA_DEMO = "Kubata Demo, Lda";

    /** Prefixo dos códigos de utilizador de teste (permite remoção segura). */
    public static final String PREFIXO_CODIGO_UTILIZADOR_DEMO = "DEMO-";
    /** Prefixo do código de barras dos artigos de amostra. */
    public static final String PREFIXO_ARTIGO_DEMO = "DEMO";
    /** Prefixo do nome das categorias de amostra. */
    public static final String PREFIXO_CATEGORIA_DEMO = "Demo - ";
    /** Faixa de NIFs reservada para clientes/fornecedores de amostra. */
    private static final String PREFIXO_NIF_CLIENTE_DEMO = "54190000";
    private static final String PREFIXO_NIF_FORNECEDOR_DEMO = "54180000";

    /** Palavra-passe única dos utilizadores de teste (visível na vista). */
    public static final String PALAVRA_PASSE_DEMO = "Demo@Kubata2026";

    private final EmpresaRepository empresaRepository;
    private final EmpresaSetupService empresaSetupService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CategoriaRepository categoriaRepository;
    private final ProdutoRepository produtoRepository;
    private final ClienteRepository clienteRepository;
    private final FornecedorRepository fornecedorRepository;

    public DemoDataService(EmpresaRepository empresaRepository,
                           EmpresaSetupService empresaSetupService,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           CategoriaRepository categoriaRepository,
                           ProdutoRepository produtoRepository,
                           ClienteRepository clienteRepository,
                           FornecedorRepository fornecedorRepository) {
        this.empresaRepository = empresaRepository;
        this.empresaSetupService = empresaSetupService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.categoriaRepository = categoriaRepository;
        this.produtoRepository = produtoRepository;
        this.clienteRepository = clienteRepository;
        this.fornecedorRepository = fornecedorRepository;
    }

    // ── API pública ───────────────────────────────────────────────────────────

    /**
     * Cria o ambiente de demonstração completo (ou apenas as opções pedidas).
     *
     * @param comEmpresa     cria a empresa fictícia com exercício fiscal aberto
     * @param comUtilizadores cria os utilizadores de teste ligados à empresa
     * @param comMestres     cria categorias, artigos, clientes e fornecedores
     * @param progresso      callback opcional para reportar cada passo à UI
     */
    @Transactional
    public DemoSummary criarDadosDemo(boolean comEmpresa,
                                      boolean comUtilizadores,
                                      boolean comMestres,
                                      Consumer<String> progresso) {
        DemoSummary resumo = new DemoSummary();
        Consumer<String> passo = progresso == null ? p -> { } : progresso;

        passo.accept("A preparar o ambiente de demonstração…");

        Empresa empresa;
        if (comEmpresa) {
            empresa = criarEmpresaDemo(resumo, passo);
        } else {
            empresa = empresaRepository.findByNif(NIF_EMPRESA_DEMO)
                    .orElseGet(() -> empresaRepository.findFirstByAtivaTrue()
                            .orElseThrow(() -> new IllegalStateException(
                                    "Não existe nenhuma empresa activa. Crie primeiro a empresa de demonstração.")));
            passo.accept("Empresa em uso: " + empresa.getNome());
        }

        if (comUtilizadores) {
            criarUtilizadoresDemo(empresa, resumo, passo);
        }

        if (comMestres) {
            criarDadosMestresDemo(resumo, passo);
        }

        passo.accept("Ambiente de demonstração concluído.");
        logger.info("Dados de demonstração criados: {}", resumo.relatorio());
        return resumo;
    }

    /**
     * Remove todos os registos de demonstração identificáveis, preservando
     * os dados reais. A empresa de demonstração é desactivada (nunca
     * eliminada) para garantir a integridade referencial.
     */
    @Transactional
    public DemoSummary limparDadosDemo(Consumer<String> progresso) {
        DemoSummary resumo = new DemoSummary();
        Consumer<String> passo = progresso == null ? p -> { } : progresso;

        passo.accept("A remover dados de demonstração…");

        // 1. Utilizadores de teste
        for (User user : userRepository.findAll()) {
            if (user.getCodigo() != null
                    && user.getCodigo().toUpperCase().startsWith(PREFIXO_CODIGO_UTILIZADOR_DEMO)) {
                String nome = user.getNome();
                userRepository.delete(user);
                resumo.registar("Utilizador removido: " + nome);
                passo.accept("Utilizador removido: " + nome);
            }
        }

        // 2. Artigos de amostra (soft delete)
        for (Produto produto : produtoRepository.findAll()) {
            if (produto.getCodigoBarra() != null
                    && produto.getCodigoBarra().toUpperCase().startsWith(PREFIXO_ARTIGO_DEMO)) {
                String nome = produto.getNome();
                produtoRepository.delete(produto);
                resumo.registar("Artigo removido: " + nome);
            }
        }
        passo.accept("Artigos de amostra removidos.");

        // 3. Categorias de amostra (soft delete)
        for (Categoria categoria : categoriaRepository.findAll()) {
            if (categoria.getNome() != null
                    && categoria.getNome().startsWith(PREFIXO_CATEGORIA_DEMO)) {
                String nome = categoria.getNome();
                categoriaRepository.delete(categoria);
                resumo.registar("Categoria removida: " + nome);
            }
        }
        passo.accept("Categorias de amostra removidas.");

        // 4. Clientes de amostra (soft delete)
        for (Cliente cliente : clienteRepository.findAll()) {
            if (cliente.getNif() != null && cliente.getNif().startsWith(PREFIXO_NIF_CLIENTE_DEMO)) {
                String nome = cliente.getNome();
                clienteRepository.delete(cliente);
                resumo.registar("Cliente removido: " + nome);
            }
        }
        passo.accept("Clientes de amostra removidos.");

        // 5. Fornecedores de amostra (soft delete)
        for (Fornecedor fornecedor : fornecedorRepository.findAll()) {
            if (fornecedor.getNif() != null && fornecedor.getNif().startsWith(PREFIXO_NIF_FORNECEDOR_DEMO)) {
                String nome = fornecedor.getNome();
                fornecedorRepository.delete(fornecedor);
                resumo.registar("Fornecedor removido: " + nome);
            }
        }
        passo.accept("Fornecedores de amostra removidos.");

        // 6. Empresa de demonstração: desactivar (nunca eliminar)
        empresaRepository.findByNif(NIF_EMPRESA_DEMO).ifPresent(empresa -> {
            if (!empresa.getAtiva()) {
                resumo.registar("A empresa de demonstração já estava inactiva.");
                return;
            }
            if (empresaRepository.countByAtivaTrue() <= 1) {
                resumo.registar("A empresa de demonstração é a única activa — mantida activa.");
                return;
            }
            empresaSetupService.alterarEstado(empresa.getId(), false);
            resumo.registar("Empresa de demonstração desactivada: " + empresa.getNome());
            passo.accept("Empresa de demonstração desactivada.");
        });

        passo.accept("Limpeza concluída.");
        logger.info("Dados de demonstração removidos: {}", resumo.relatorio());
        return resumo;
    }

    /**
     * Contagem dos registos de demonstração actualmente presentes na base
     * de dados — usada pela vista para mostrar o estado antes de agir.
     */
    @Transactional(readOnly = true)
    public Map<String, Long> contagemDemonstracao() {
        Map<String, Long> contagens = new LinkedHashMap<>();
        contagens.put("empresa", empresaRepository.findByNif(NIF_EMPRESA_DEMO).isPresent() ? 1L : 0L);
        contagens.put("utilizadores", userRepository.findAll().stream()
                .filter(u -> u.getCodigo() != null
                        && u.getCodigo().toUpperCase().startsWith(PREFIXO_CODIGO_UTILIZADOR_DEMO))
                .count());
        contagens.put("categorias", categoriaRepository.findAll().stream()
                .filter(c -> c.getNome() != null && c.getNome().startsWith(PREFIXO_CATEGORIA_DEMO))
                .count());
        contagens.put("artigos", produtoRepository.findAll().stream()
                .filter(p -> p.getCodigoBarra() != null
                        && p.getCodigoBarra().toUpperCase().startsWith(PREFIXO_ARTIGO_DEMO))
                .count());
        contagens.put("clientes", clienteRepository.findAll().stream()
                .filter(c -> c.getNif() != null && c.getNif().startsWith(PREFIXO_NIF_CLIENTE_DEMO))
                .count());
        contagens.put("fornecedores", fornecedorRepository.findAll().stream()
                .filter(f -> f.getNif() != null && f.getNif().startsWith(PREFIXO_NIF_FORNECEDOR_DEMO))
                .count());
        return contagens;
    }

    // ── Empresa ───────────────────────────────────────────────────────────────

    private Empresa criarEmpresaDemo(DemoSummary resumo, Consumer<String> passo) {
        Empresa existente = empresaRepository.findByNif(NIF_EMPRESA_DEMO).orElse(null);
        if (existente != null) {
            if (!existente.getAtiva()) {
                existente.setAtiva(true);
                existente = empresaRepository.save(existente);
            }
            passo.accept("Empresa de demonstração já existe: " + existente.getNome());
            resumo.registarReutilizado("Empresa de demonstração reutilizada.");
            return existente;
        }

        passo.accept("A criar a empresa de demonstração…");

        Empresa empresa = new Empresa();
        empresa.setNome(NOME_EMPRESA_DEMO);
        empresa.setNomeComercial("Kubata Demo");
        empresa.setNif(NIF_EMPRESA_DEMO);
        empresa.setIdentificador(CODIGO_EMPRESA_DEMO);
        empresa.setTipoContribuinte("Pessoa Colectiva");
        empresa.setMorada("Rua da Demonstração, 100 — Talatona");
        empresa.setCodigoPostal("CP 10044");
        empresa.setMunicipio("Luanda");
        empresa.setProvincia("Luanda");
        empresa.setPais("AO");
        empresa.setTelefone("923 000 000");
        empresa.setEmail("demo@kubata.ao");
        empresa.setWebsite("https://demo.kubata.ao");
        empresa.setRegimeFiscal("Regime Geral");
        empresa.setCae("47110");
        empresa.setDescricaoActividade("Comércio a retalho e serviços — ambiente de demonstração.");
        empresa.setMoedaBase("AOA");
        empresa.setAtiva(true);
        empresa.setPredefinida(false);

        Empresa salva = empresaSetupService.saveAndProvision(
                empresa,
                true,     // abrir exercício fiscal corrente
                false,    // sem plano de backup automático no ambiente de teste
                "DAILY",
                30,
                false,    // MFA não obrigatório no ambiente de demonstração
                Map.of("AMBIENTE", "DEMONSTRACAO"),
                "Dados de Demonstração"
        );

        resumo.registarCriado("Empresa de demonstração criada: " + salva.getNome());
        passo.accept("Empresa criada com exercício fiscal "
                + salva.getExercicioActual() + " aberto.");
        return salva;
    }

    // ── Utilizadores de teste ─────────────────────────────────────────────────

    private void criarUtilizadoresDemo(Empresa empresa, DemoSummary resumo, Consumer<String> passo) {
        passo.accept("A criar os utilizadores de teste…");

        String hash = passwordEncoder.encode(PALAVRA_PASSE_DEMO);

        criarUtilizadorDemo(empresa, "DEMO-ADMIN", "Administrador Demo",
                "admin.demo@kubata.ao", Role.ADMIN, "Tecnologia da Informação", "Administrador do sistema", hash, resumo);
        criarUtilizadorDemo(empresa, "DEMO-VENDEDOR", "Vendedor Demo",
                "vendedor.demo@kubata.ao", Role.VENDEDOR, "Comercial", "Vendedor de balcão", hash, resumo);
        criarUtilizadorDemo(empresa, "DEMO-CAIXA", "Caixa Demo",
                "caixa.demo@kubata.ao", Role.CAIXA, "Comercial", "Operador de caixa", hash, resumo);
        criarUtilizadorDemo(empresa, "DEMO-CONTAB", "Contabilista Demo",
                "contabilista.demo@kubata.ao", Role.CONTABILISTA, "Financeiro", "Contabilista certificado", hash, resumo);

        passo.accept("Utilizadores de teste prontos (palavra-passe única: " + PALAVRA_PASSE_DEMO + ").");
    }

    private void criarUtilizadorDemo(Empresa empresa,
                                     String codigo,
                                     String nome,
                                     String email,
                                     Role role,
                                     String departamento,
                                     String cargo,
                                     String hash,
                                     DemoSummary resumo) {
        if (userRepository.existsByEmailIgnoreCase(email)
                || userRepository.existsByCodigoIgnoreCase(codigo)) {
            resumo.registarReutilizado("Utilizador já existente: " + email);
            return;
        }

        User user = new User();
        user.setCodigo(codigo);
        user.setNome(nome);
        user.setEmail(email);
        user.setPassword(hash);
        user.setRole(role);
        user.setTipoConta(TipoConta.PESSOAL);
        user.setEmpresa(empresa);
        user.setDepartamento(departamento);
        user.setCargo(cargo);
        user.setActive(true);
        user.setSuperadmin(false);
        user.setIdioma("pt-AO");
        user.setTema("VERDE_ADMIN");
        user.setLinhasPorPagina(50);
        // Ambiente de demonstração: login imediato, sem troca de palavra-passe.
        user.setPasswordProvisoria(false);

        userRepository.save(user);
        resumo.registarCriado("Utilizador criado: " + email + " (" + role + ")");
    }

    // ── Dados mestres de amostra ──────────────────────────────────────────────

    private void criarDadosMestresDemo(DemoSummary resumo, Consumer<String> passo) {
        passo.accept("A criar categorias de amostra…");
        Map<String, Categoria> categorias = criarCategoriasDemo(resumo);

        passo.accept("A criar artigos de amostra…");
        criarArtigosDemo(categorias, resumo);

        passo.accept("A criar clientes de amostra…");
        criarClientesDemo(resumo);

        passo.accept("A criar fornecedores de amostra…");
        criarFornecedoresDemo(resumo);
    }

    private Map<String, Categoria> criarCategoriasDemo(DemoSummary resumo) {
        Map<String, Categoria> categorias = new LinkedHashMap<>();
        String[][] dados = {
                {"Demo - Bebidas", "Águas, refrigerantes e sumos de amostra."},
                {"Demo - Alimentar", "Produtos alimentares básicos de amostra."},
                {"Demo - Eletrónica", "Equipamento electrónico de amostra."},
                {"Demo - Escritório", "Artigos de escritório de amostra."}
        };

        for (String[] dado : dados) {
            Categoria categoria = categoriaRepository.findByNome(dado[0]).orElse(null);
            if (categoria == null) {
                categoria = new Categoria();
                categoria.setNome(dado[0]);
                categoria.setDescricao(dado[1]);
                categoria = categoriaRepository.save(categoria);
                resumo.registarCriado("Categoria criada: " + dado[0]);
            } else {
                resumo.registarReutilizado("Categoria já existente: " + dado[0]);
            }
            categorias.put(dado[0], categoria);
        }
        return categorias;
    }

    private void criarArtigosDemo(Map<String, Categoria> categorias, DemoSummary resumo) {
        // {código, nome, categoria, preço venda, preço compra, stock}
        String[][] dados = {
                {"DEMO-001", "Água Mineral 1,5L", "Demo - Bebidas", "350", "220", "480"},
                {"DEMO-002", "Refrigerante Cola 2L", "Demo - Bebidas", "950", "640", "240"},
                {"DEMO-003", "Sumo de Manga 1L", "Demo - Bebidas", "780", "520", "180"},
                {"DEMO-004", "Arroz Agulha 5kg", "Demo - Alimentar", "4200", "3100", "120"},
                {"DEMO-005", "Óleo de Soja 1L", "Demo - Alimentar", "1850", "1380", "150"},
                {"DEMO-006", "Açúcar Refinado 1kg", "Demo - Alimentar", "690", "480", "300"},
                {"DEMO-007", "Café Torrado 1kg", "Demo - Alimentar", "5400", "3900", "90"},
                {"DEMO-008", "Auriculares Bluetooth", "Demo - Eletrónica", "12500", "8200", "40"},
                {"DEMO-009", "Carregador USB-C 20W", "Demo - Eletrónica", "8900", "5700", "65"},
                {"DEMO-010", "Rato Sem Fios", "Demo - Eletrónica", "7300", "4600", "55"},
                {"DEMO-011", "Resma Papel A4", "Demo - Escritório", "2900", "2100", "200"},
                {"DEMO-012", "Canetas Esferro (10un)", "Demo - Escritório", "1500", "900", "250"}
        };

        for (String[] dado : dados) {
            if (produtoRepository.existsByCodigoBarra(dado[0])) {
                resumo.registarReutilizado("Artigo já existente: " + dado[0]);
                continue;
            }

            Produto produto = new Produto();
            produto.setCodigoBarra(dado[0]);
            produto.setNome(dado[1]);
            produto.setDescricao("Artigo de demonstração — " + dado[1]);
            produto.setCategoria(categorias.get(dado[2]));
            produto.setPrecoUnitario(new BigDecimal(dado[3]));
            produto.setPrecoCompra(new BigDecimal(dado[4]));
            produto.setStock(Integer.parseInt(dado[5]));
            produto.setStockMinimo(10);
            produto.setUnidadeMedida(UnidadeMedida.UNIDADE);
            produto.setPercentualIva(new BigDecimal("14.00"));
            produto.setSujeitoRetencao(false);
            produto.setMarca("Kubata Demo");

            produtoRepository.save(produto);
            resumo.registarCriado("Artigo criado: " + dado[0] + " — " + dado[1]);
        }
    }

    private void criarClientesDemo(DemoSummary resumo) {
        // {nome, NIF, cidade, telefone, tipo}
        Object[][] dados = {
                {"Supermercado Kilamba", "5419000001", "Luanda", "923 100 101", TipoCliente.EMPRESA},
                {"Farmácia Central Lda", "5419000002", "Benguela", "923 100 102", TipoCliente.EMPRESA},
                {"Hotel Baía Azul", "5419000003", "Namibe", "923 100 103", TipoCliente.EMPRESA},
                {"João M. Fernandes", "5419000004", "Luanda", "923 100 104", TipoCliente.SINGULAR},
                {"Maria do Carmo Neto", "5419000005", "Huambo", "923 100 105", TipoCliente.SINGULAR},
                {"Consumidor Final", "5419000006", "Luanda", "923 100 106", TipoCliente.CONSUMIDOR_FINAL}
        };

        for (Object[] dado : dados) {
            String nif = (String) dado[1];
            if (clienteRepository.existsByNif(nif)) {
                resumo.registarReutilizado("Cliente já existente: " + nif);
                continue;
            }

            Cliente cliente = new Cliente();
            cliente.setNome((String) dado[0]);
            cliente.setNif(nif);
            cliente.setCidade((String) dado[2]);
            cliente.setTelefone((String) dado[3]);
            cliente.setTipo((TipoCliente) dado[4]);
            cliente.setEndereco("Rua da Demonstração, " + nif.substring(nif.length() - 3));
            cliente.setEmail("contato@" + nif + ".demo.kubata.ao");
            cliente.setLimiteCredito(new BigDecimal("250000"));
            cliente.setCategoriaCliente("Demo");
            cliente.setProvincia("Demo");

            clienteRepository.save(cliente);
            resumo.registarCriado("Cliente criado: " + dado[0] + " (NIF " + nif + ")");
        }
    }

    private void criarFornecedoresDemo(DemoSummary resumo) {
        // {nome, NIF, cidade, pessoa de contacto}
        String[][] dados = {
                {"Distribuidora Alimentar Lda", "5418000001", "Luanda", "Paulo António"},
                {"Importadora Tech Lda", "5418000002", "Luanda", "Sandra Miguel"},
                {"Papelaria Central Lda", "5418000003", "Benguela", "Carlos Eduardo"},
                {"Bebidas do Planalto Lda", "5418000004", "Huambo", "Ana Beatriz"}
        };

        for (String[] dado : dados) {
            if (fornecedorRepository.existsByNif(dado[1])) {
                resumo.registarReutilizado("Fornecedor já existente: " + dado[1]);
                continue;
            }

            Fornecedor fornecedor = new Fornecedor();
            fornecedor.setNome(dado[0]);
            fornecedor.setNif(dado[1]);
            fornecedor.setCidade(dado[2]);
            fornecedor.setPessoaContacto(dado[3]);
            fornecedor.setEndereco("Rua Industrial, " + dado[1].substring(dado[1].length() - 3));
            fornecedor.setTelefone("923 200 " + dado[1].substring(dado[1].length() - 4));
            fornecedor.setEmail("contacto@" + dado[1] + ".demo.kubata.ao");
            fornecedor.setObservacoes("Fornecedor de demonstração.");

            fornecedorRepository.save(fornecedor);
            resumo.registarCriado("Fornecedor criado: " + dado[0] + " (NIF " + dado[1] + ")");
        }
    }

    // ── Resultado ─────────────────────────────────────────────────────────────

    /** Resumo accionável da operação, para apresentar ao administrador. */
    public static class DemoSummary {
        private final List<String> criados = new ArrayList<>();
        private final List<String> reutilizados = new ArrayList<>();

        void registarCriado(String mensagem) { criados.add(mensagem); }
        void registarReutilizado(String mensagem) { reutilizados.add(mensagem); }
        void registar(String mensagem) { reutilizados.add(mensagem); }

        public List<String> getCriados() { return List.copyOf(criados); }
        public List<String> getReutilizados() { return List.copyOf(reutilizados); }
        public int getTotalCriados() { return criados.size(); }

        public String relatorio() {
            return criados.size() + " registos criados, " + reutilizados.size() + " pré-existentes/tratados.";
        }
    }
}
