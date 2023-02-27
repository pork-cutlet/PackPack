package mandarin.packpack.supporter.calculation;

import javax.annotation.Nonnull;
import java.math.BigDecimal;

public class NumericalResult {
    @Nonnull
    public final BigDecimal value;
    @Nonnull
    public final BigDecimal error;
    @Nonnull
    public final Formula.ALGORITHM algorithm;


    public String warning;

    public NumericalResult(@Nonnull BigDecimal value, @Nonnull BigDecimal error, @Nonnull Formula.ALGORITHM algorithm) {
        this.value = value;
        this.error = error;
        this.algorithm = algorithm;
    }
}
