package mandarin.packpack.commands.math;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.commands.TimedConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.ImageDrawing;
import mandarin.packpack.supporter.calculation.Equation;
import mandarin.packpack.supporter.calculation.Formula;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RTheta extends TimedConstraintCommand {
    public RTheta(ConstraintCommand.ROLE role, int lang, @Nullable IDHolder idHolder, long time) {
        super(role, lang, idHolder, time, StaticStore.COMMAND_RTHETA_ID, false);
    }

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        String[] contents = getContent(event).split(" ");

        if(contents.length < 2) {
            replyToMessageSafely(ch, LangID.getStringByID("plot_formula", lang), getMessage(event), a -> a);

            return;
        }

        BigDecimal[] xRange = getXRange(getContent(event));
        BigDecimal[] yRange = getYRange(getContent(event));
        BigDecimal[] tRange = getTRange(getContent(event));
        BigDecimal[] rRange = getRRange(getContent(event));

        String f = filterFormula(getContent(event));

        String[] test = f.split("=");

        if(test.length > 2) {
            replyToMessageSafely(ch, LangID.getStringByID("plot_invalid", lang), getMessage(event), a -> a);

            return;
        } else if(test.length == 2) {
            f = test[0] + " - (" + test[1] + ")";
        }

        Formula formula = new Formula(f, 2, lang);

        if(!Formula.error.isEmpty()) {
            replyToMessageSafely(ch, Formula.getErrorMessage(), getMessage(event), a -> a);

            return;
        }

        if(xRange == null) {
            xRange = new BigDecimal[2];

            xRange[0] = new BigDecimal("-5");
            xRange[1] = new BigDecimal("5");
        } else if(xRange[1].subtract(xRange[0]).compareTo(BigDecimal.ZERO) == 0) {
            xRange[0] = new BigDecimal("-5");
            xRange[1] = new BigDecimal("5");
        }

        if(yRange == null) {
            yRange = xRange.clone();
        }

        if(tRange == null) {
            tRange = new BigDecimal[] {
                    BigDecimal.ZERO,
                    BigDecimal.valueOf(Math.PI * 2)
            };
        }

        BigDecimal best = xRange[0].abs().max(xRange[1].abs()).max(yRange[0].abs()).max(yRange[1].abs()).multiply(BigDecimal.valueOf(2).sqrt(Equation.context));

        if(rRange == null || rRange[0].compareTo(rRange[1]) == 0) {
            rRange = new BigDecimal[] {
                    best.negate(),
                    best
            };
        }

        double[] xr = new double[2];
        double[] yr = new double[2];
        double[] tr = new double[2];
        double[] rr = new double[2];

        for(int i = 0; i < xr.length; i++) {
            xr[i] = xRange[i].min(BigDecimal.valueOf(Double.MAX_VALUE)).max(BigDecimal.valueOf(-Double.MAX_VALUE)).doubleValue();
            yr[i] = yRange[i].min(BigDecimal.valueOf(Double.MAX_VALUE)).max(BigDecimal.valueOf(-Double.MAX_VALUE)).doubleValue();
            tr[i] = tRange[i].min(BigDecimal.valueOf(Double.MAX_VALUE)).max(BigDecimal.valueOf(-Double.MAX_VALUE)).doubleValue();
            rr[i] = rRange[i].min(BigDecimal.valueOf(Double.MAX_VALUE)).max(BigDecimal.valueOf(-Double.MAX_VALUE)).doubleValue();
        }

        Object[] plots = ImageDrawing.plotRThetaGraph(formula, xr, yr, rr, tr, lang);

        if(plots == null) {
            replyToMessageSafely(ch, LangID.getStringByID("plot_fail", lang), getMessage(event), a -> a);
        } else {
            sendMessageWithFile(ch, (String) plots[1], (File) plots[0], "plot.png", getMessage(event));
        }
    }

    private BigDecimal[] getXRange(String command) {
        Pattern pattern = Pattern.compile("-xr(\\s+)?\\[.+?,.+?]");
        Matcher matcher = pattern.matcher(command);

        if(matcher.find()) {
            String filtered = matcher.group().replaceAll("-xr(\\s+)?", "").replaceAll("\\s", "");

            String[] removed = filtered.substring(1, filtered.length() - 1).split(",");

            if(removed.length == 2) {
                BigDecimal minimum = Equation.calculate(removed[0], null, false, lang);

                if(!Equation.error.isEmpty()) {
                    Equation.error.clear();

                    return null;
                }

                BigDecimal maximum = Equation.calculate(removed[1], null, false, lang);

                if(!Equation.error.isEmpty()) {
                    Equation.error.clear();

                    return null;
                }

                return new BigDecimal[] {minimum.min(maximum), maximum.max(minimum)};
            }
        }

        return null;
    }

    private BigDecimal[] getYRange(String command) {
        Pattern pattern = Pattern.compile("-yr(\\s+)?\\[.+?,.+?]");
        Matcher matcher = pattern.matcher(command);

        if(matcher.find()) {
            String filtered = matcher.group().replaceAll("-yr(\\s+)?", "").replaceAll("\\s", "");

            String[] removed = filtered.substring(1, filtered.length() - 1).split(",");

            if(removed.length == 2) {
                BigDecimal minimum = Equation.calculate(removed[0], null, false, lang);

                if(!Equation.error.isEmpty()) {
                    Equation.error.clear();

                    return null;
                }

                BigDecimal maximum = Equation.calculate(removed[1], null, false, lang);

                if(!Equation.error.isEmpty()) {
                    Equation.error.clear();

                    return null;
                }

                return new BigDecimal[] {minimum.min(maximum), maximum.max(minimum)};
            }
        }

        return null;
    }

    private BigDecimal[] getTRange(String command) {
        Pattern pattern = Pattern.compile("-tr(\\s+)?\\[.+?,.+?]");
        Matcher matcher = pattern.matcher(command);

        if(matcher.find()) {
            String filtered = matcher.group().replaceAll("-tr(\\s+)?", "").replaceAll("\\s", "");

            String[] removed = filtered.substring(1, filtered.length() - 1).split(",");

            if(removed.length == 2) {
                BigDecimal minimum = Equation.calculate(removed[0], null, false, lang);

                if(!Equation.error.isEmpty()) {
                    Equation.error.clear();

                    return null;
                }

                BigDecimal maximum = Equation.calculate(removed[1], null, false, lang);

                if(!Equation.error.isEmpty()) {
                    Equation.error.clear();

                    return null;
                }

                return new BigDecimal[] {minimum.min(maximum), maximum.max(minimum)};
            }
        }

        return null;
    }

    private BigDecimal[] getRRange(String command) {
        Pattern pattern = Pattern.compile("-rr(\\s+)?\\[.+?,.+?]");
        Matcher matcher = pattern.matcher(command);

        if(matcher.find()) {
            String filtered = matcher.group().replaceAll("-rr(\\s+)?", "").replaceAll("\\s", "");

            String[] removed = filtered.substring(1, filtered.length() - 1).split(",");

            if(removed.length == 2) {
                BigDecimal minimum = Equation.calculate(removed[0], null, false, lang);

                if(!Equation.error.isEmpty()) {
                    Equation.error.clear();

                    return null;
                }

                BigDecimal maximum = Equation.calculate(removed[1], null, false, lang);

                if(!Equation.error.isEmpty()) {
                    Equation.error.clear();

                    return null;
                }

                return new BigDecimal[] {minimum.min(maximum), maximum.max(minimum)};
            }
        }

        return null;
    }

    private String filterFormula(String command) {
        String removePrefix = command.split(" ", 2)[1];

        return removePrefix.replaceAll("-(r|ratio)\\s", "").replaceAll("-[xytr]r(\\s+)?\\[.+?,.+?]", "");
    }
}
