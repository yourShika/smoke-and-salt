package de.yourshika.smokeandsalt.cooking;

import org.bukkit.Material;

/**
 * Ein einzelnes Koch-Rezept. Beschreibt, welche Zutat an welcher Station in wie
 * vielen Ticks zu welchem Ergebnis wird.
 *
 * <p>Zutat und Ergebnis koennen jeweils ein Custom-Item (ueber die
 * {@code item_id}) oder ein Vanilla-{@link Material} sein. Fuer sequentielles
 * Kochen (mehrstufige Verarbeitung) verweist die Zutat einer Stufe einfach auf
 * das Custom-Ergebnis der vorherigen Stufe.</p>
 *
 * <p>Die {@link CookingRegistry} ist standardmaessig leer - konkrete Rezepte
 * werden spaeter ueber die config.yml oder die API hinzugefuegt.</p>
 */
public final class CookingRecipe {

    private final String id;
    private final CookingStation station;
    private final String inputItemId;      // Custom-Item-ID oder null
    private final Material inputMaterial;   // Vanilla-Material oder null
    private final String resultItemId;      // Custom-Item-ID oder null
    private final Material resultMaterial;  // Vanilla-Material oder null
    private final int resultAmount;
    private final int resultAmountMin;
    private final int resultAmountMax;
    private final int durationTicks;

    private CookingRecipe(Builder b) {
        this.id = b.id;
        this.station = b.station;
        this.inputItemId = b.inputItemId;
        this.inputMaterial = b.inputMaterial;
        this.resultItemId = b.resultItemId;
        this.resultMaterial = b.resultMaterial;
        this.resultAmount = Math.max(1, b.resultAmount);
        // Zufaellige Ausbeute: fehlt min/max, wird die feste Menge genutzt.
        int min = b.resultAmountMin > 0 ? b.resultAmountMin : this.resultAmount;
        int max = b.resultAmountMax > 0 ? b.resultAmountMax : this.resultAmount;
        this.resultAmountMin = Math.max(1, Math.min(min, max));
        this.resultAmountMax = Math.max(this.resultAmountMin, max);
        this.durationTicks = Math.max(1, b.durationTicks);
    }

    public String id() { return id; }
    public CookingStation station() { return station; }
    public String inputItemId() { return inputItemId; }
    public Material inputMaterial() { return inputMaterial; }
    public String resultItemId() { return resultItemId; }
    public Material resultMaterial() { return resultMaterial; }
    public int resultAmount() { return resultAmount; }
    public int resultAmountMin() { return resultAmountMin; }
    public int resultAmountMax() { return resultAmountMax; }
    /** Zieht eine zufaellige Ausbeute im Bereich [min, max]. */
    public int rollResultAmount() {
        if (resultAmountMax <= resultAmountMin) return resultAmountMin;
        return resultAmountMin + (int) (Math.random() * (resultAmountMax - resultAmountMin + 1));
    }
    public int durationTicks() { return durationTicks; }

    public boolean inputIsCustom() { return inputItemId != null; }
    public boolean resultIsCustom() { return resultItemId != null; }

    public static Builder builder(String id, CookingStation station) {
        return new Builder(id, station);
    }

    /** Fluent-Builder fuer {@link CookingRecipe}. */
    public static final class Builder {
        private final String id;
        private final CookingStation station;
        private String inputItemId;
        private Material inputMaterial;
        private String resultItemId;
        private Material resultMaterial;
        private int resultAmount = 1;
        private int resultAmountMin = 0;
        private int resultAmountMax = 0;
        private int durationTicks = 100;

        private Builder(String id, CookingStation station) {
            this.id = id;
            this.station = station;
        }

        public Builder inputItem(String customItemId) {
            this.inputItemId = customItemId;
            return this;
        }

        public Builder inputMaterial(Material material) {
            this.inputMaterial = material;
            return this;
        }

        public Builder resultItem(String customItemId) {
            this.resultItemId = customItemId;
            return this;
        }

        public Builder resultMaterial(Material material) {
            this.resultMaterial = material;
            return this;
        }

        public Builder resultAmount(int amount) {
            this.resultAmount = amount;
            return this;
        }

        public Builder resultAmountRange(int min, int max) {
            this.resultAmountMin = min;
            this.resultAmountMax = max;
            return this;
        }

        public Builder duration(int ticks) {
            this.durationTicks = ticks;
            return this;
        }

        public CookingRecipe build() {
            if (inputItemId == null && inputMaterial == null) {
                throw new IllegalStateException("Rezept '" + id + "' benoetigt eine Zutat.");
            }
            if (resultItemId == null && resultMaterial == null) {
                throw new IllegalStateException("Rezept '" + id + "' benoetigt ein Ergebnis.");
            }
            return new CookingRecipe(this);
        }
    }
}
