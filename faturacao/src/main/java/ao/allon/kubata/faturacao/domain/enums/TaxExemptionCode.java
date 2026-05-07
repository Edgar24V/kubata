package ao.allon.kubata.faturacao.domain.enums;

public enum TaxExemptionCode {
    M00("M00", "Regime Simplificado"),
    M02("M02", "Transmissão de bens e prestação de serviços não sujeita"),
    M04("M04", "Isento Artigo 13.º"),
    M05("M05", "Isento Artigo 14.º"),
    M06("M06", "Isento Artigo 15.º"),
    M07("M07", "Isento Artigo 16.º"),
    M10("M10", "Isento Artigo 12.º alínea a)"),
    M11("M11", "Isento Artigo 12.º alínea b)"),
    M12("M12", "Isento Artigo 12.º alínea c)"),
    M13("M13", "Isento Artigo 12.º alínea d)"),
    M14("M14", "Isento Artigo 12.º alínea e)"),
    M15("M15", "Isento Artigo 12.º alínea f)"),
    M16("M16", "Isento Artigo 12.º alínea g)"),
    M17("M17", "Isento Artigo 12.º alínea h)"),
    M18("M18", "Isento Artigo 12.º alínea i)"),
    M19("M19", "Isento Artigo 12.º alínea j)"),
    M20("M20", "Isento Artigo 12.º alínea k)"),
    M21("M21", "Isento Artigo 12.º alínea l)"),
    M22("M22", "Isento Artigo 12.º alínea m)"),
    M23("M23", "Isento Artigo 12.º alínea n)"),
    M24("M24", "Isento Artigo 12.º alínea o)"),
    M30("M30", "Isento Artigo 12.º alínea p)"),
    M31("M31", "Isento Artigo 12.º alínea q)"),
    M32("M32", "Isento Artigo 12.º alínea r)"),
    M33("M33", "Isento Artigo 12.º alínea s)"),
    M80("M80", "Isento Artigo 12.º alínea t)"),
    M81("M81", "Isento Artigo 12.º alínea u)"),
    M82("M82", "Isento Artigo 12.º alínea v)"),
    M83("M83", "Isento Artigo 12.º alínea w)"),
    M84("M84", "Isento Artigo 12.º alínea x)"),
    M99("M99", "Outros (Não sujeição ou Isenção)");

    private final String code;
    private final String description;

    TaxExemptionCode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static boolean isValid(String code) {
        for (TaxExemptionCode c : values()) {
            if (c.code.equals(code)) return true;
        }
        return false;
    }
    
    @Override
    public String toString() {
        return code + " - " + description;
    }
}
