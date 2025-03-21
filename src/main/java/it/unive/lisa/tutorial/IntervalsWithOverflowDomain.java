package it.unive.lisa.tutorial;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.AdditionOperator;
import it.unive.lisa.symbolic.value.operator.DivisionOperator;
import it.unive.lisa.symbolic.value.operator.MultiplicationOperator;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import it.unive.lisa.symbolic.value.operator.binary.*;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Objects;

public class IntervalsWithOverflowDomain implements BaseNonRelationalValueDomain<IntervalsWithOverflowDomain> {

    private static final int MIN = Integer.MIN_VALUE;
    private static final int MAX = Integer.MAX_VALUE;
    private static final long RANGE_32BIT = 1L << 32;

    private final int low;
    private final int high;

    public IntervalsWithOverflowDomain() {
        this.low = MIN;
        this.high = MAX;
    }

    public IntervalsWithOverflowDomain(int low, int high) {
        this.low = low;
        this.high = high;
    }

    @Override
    public IntervalsWithOverflowDomain top() {
        return new IntervalsWithOverflowDomain(MIN, MAX);
    }

    @Override
    public IntervalsWithOverflowDomain bottom() {
        return new IntervalsWithOverflowDomain(1, 0);
    }

    @Override
    public boolean isTop() {
        return low == MIN && high == MAX;
    }

    @Override
    public boolean isBottom() {
        return low > high;
    }

    @Override
    public IntervalsWithOverflowDomain lubAux(IntervalsWithOverflowDomain other) throws SemanticException {
        if (this.isBottom() || other.isBottom()) {
            return bottom();
        }
        return new IntervalsWithOverflowDomain(
                Math.min(this.low, other.low),
                Math.max(this.high, other.high)
        );
    }

    @Override
    public IntervalsWithOverflowDomain wideningAux(IntervalsWithOverflowDomain other) throws SemanticException {
        if (this.isBottom() || other.isBottom()) {
            return bottom();
        }

        long newLow = Math.min((long) this.low, (long) other.low);
        long newHigh = Math.max((long) this.high, (long) other.high);

        if (other.low < this.low) {
            long delta = (long) this.low - other.low;
            newLow = wrapAround32Bit(newLow - delta); // Réduire avec wrap-around
        }
        if (other.high > this.high) {
            long delta = (long) other.high - this.high;
            newHigh = wrapAround32Bit(newHigh + delta);
        }

        if (newLow > newHigh) {
            return top();
        }

        return new IntervalsWithOverflowDomain((int) newLow, (int) newHigh);
    }


    /*@Override
public IntervalsWithOverflowDomain wideningAux(IntervalsWithOverflowDomain other) throws SemanticException {
    if (this.isBottom() || other.isBottom()) {
        return bottom();
    }

    long newLow = Math.min((long) this.low, (long) other.low);
    long newHigh = Math.max((long) this.high, (long) other.high);

    // Élargir plus fortement pour converger
    if (other.low < this.low) {
        newLow = wrapAround32Bit(newLow - (this.low - other.low));
    }
    if (other.high > this.high) {
        newHigh = wrapAround32Bit(newHigh + (other.high - this.high));
    }

    return new IntervalsWithOverflowDomain((int) newLow, (int) newHigh);
}*/

    /*@Override
    public boolean lessOrEqualAux(IntervalsWithOverflowDomain other) throws SemanticException {
        if (this.isBottom() && other.isBottom()) {
            return true; // ⊥ ≤ ⊥
        }
        if (this.isBottom() || other.isBottom()) {
            return false; // Si un seul est ⊥, pas évaluable, donc pas ≤
        }
        return other.low <= this.low && this.high <= other.high;
    }*/
    @Override
    public boolean lessOrEqualAux(IntervalsWithOverflowDomain other) throws SemanticException {
        if (this.isBottom()) {
            return true; // ⊥ ≤ tout (y compris ⊥ et top)
        }
        if (other.isBottom()) {
            return false; // a ≤ ⊥ est faux sauf si a est ⊥
        }
        return other.low <= this.low && this.high <= other.high;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        IntervalsWithOverflowDomain other = (IntervalsWithOverflowDomain) obj;
        return this.low == other.low && this.high == other.high;
    }

    @Override
    public int hashCode() {
        return Objects.hash(low, high);
    }

    @Override
    public String toString() {
        if (isBottom()) return "⊥";
        if (isTop()) return "T";
        return "[" + low + ", " + high + "]";
    }

    @Override
    public IntervalsWithOverflowDomain evalNonNullConstant(Constant constant, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        if (constant.getValue() instanceof Integer) {
            int value = (Integer) constant.getValue();
            System.out.println("evalConstant: value = " + value);
            return new IntervalsWithOverflowDomain(value, value);
        }
        System.out.println("evalConstant: non-integer, returning top");
        return top();
    }

    @Override
    public IntervalsWithOverflowDomain evalUnaryExpression(UnaryOperator operator, IntervalsWithOverflowDomain arg, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        if (arg.isBottom()) {
            return bottom();
        }
        if (operator instanceof NumericNegation) {
            long newLow = -(long) arg.high;
            long newHigh = -(long) arg.low;
            if (newLow > MAX || newLow < MIN || newHigh > MAX || newHigh < MIN) {
                return top();
            }
            return new IntervalsWithOverflowDomain((int) newHigh, (int) newLow);
        }
        return top();
    }

    @Override
    public IntervalsWithOverflowDomain evalBinaryExpression(BinaryOperator operator, IntervalsWithOverflowDomain left, IntervalsWithOverflowDomain right, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        System.out.println("evalBinary: left = " + left + ", right = " + right + ", operator = " + operator);
        if (left.isBottom() || right.isBottom()) {
            System.out.println("evalBinary: returning bottom");
            return bottom();
        }
        if (operator instanceof AdditionOperator) {
            return add(left, right);
        } else if (operator instanceof SubtractionOperator) {
            return sub(left, right);
        } else if (operator instanceof MultiplicationOperator) {
            return mul(left, right);
        } else if (operator instanceof DivisionOperator) {
            return div(left, right);
        }
        System.out.println("evalBinary: unknown operator, returning top");
        return top();
    }


    @Override
    public ValueEnvironment<IntervalsWithOverflowDomain> assumeBinaryExpression(
            ValueEnvironment<IntervalsWithOverflowDomain> environment,
            BinaryOperator operator,
            ValueExpression left,
            ValueExpression right,
            ProgramPoint src,
            ProgramPoint dest,
            SemanticOracle oracle) throws SemanticException {
        if (environment.isBottom()) {
            return environment;
        }

        IntervalsWithOverflowDomain leftValue = environment.eval(left, src, oracle);
        IntervalsWithOverflowDomain rightValue = environment.eval(right, src, oracle);

        if (leftValue.isBottom() || rightValue.isBottom()) {
            return environment.bottom();
        }

        Identifier id = null;
        IntervalsWithOverflowDomain eval = null;
        boolean rightIsExpr = false;

        if (left instanceof Identifier) {
            id = (Identifier) left;
            eval = rightValue;
            rightIsExpr = true;
        } else if (right instanceof Identifier) {
            id = (Identifier) right;
            eval = leftValue;
            rightIsExpr = false;
        } else {
            return environment;
        }

        IntervalsWithOverflowDomain starting = environment.getState(id);
        if (starting == null) starting = top();

        IntervalsWithOverflowDomain update = null;

        if (operator instanceof ComparisonEq) {
            if (starting.low <= eval.low && starting.high >= eval.high) {
                update = new IntervalsWithOverflowDomain(eval.low, eval.high);
            } else {
                update = bottom();
            }
        } else if (operator instanceof ComparisonGe) { // x >= n
            if (rightIsExpr) {
                long newLow = Math.max((long) starting.low, (long) eval.low);
                long newHigh = starting.high;
                update = new IntervalsWithOverflowDomain((int) wrapAround32Bit(newLow), (int) wrapAround32Bit(newHigh));
            } else { // n >= x (x <= n)
                long newLow = starting.low;
                long newHigh = Math.min((long) starting.high, (long) eval.high);
                update = new IntervalsWithOverflowDomain((int) wrapAround32Bit(newLow), (int) wrapAround32Bit(newHigh));
            }
        } else if (operator instanceof ComparisonGt) { // x > n
            if (rightIsExpr) {
                long newLow = Math.max((long) starting.low, (long) eval.high + 1);
                long newHigh = starting.high;
                update = new IntervalsWithOverflowDomain((int) wrapAround32Bit(newLow), (int) wrapAround32Bit(newHigh));
            } else { // n > x (x < n)
                long newLow = starting.low;
                long newHigh = Math.min((long) starting.high, (long) eval.low - 1);
                update = new IntervalsWithOverflowDomain((int) wrapAround32Bit(newLow), (int) wrapAround32Bit(newHigh));
            }
        } else if (operator instanceof ComparisonLe) { // x <= n
            if (rightIsExpr) {
                long newLow = starting.low;
                long newHigh = Math.min((long) starting.high, (long) eval.high);
                update = new IntervalsWithOverflowDomain((int) wrapAround32Bit(newLow), (int) wrapAround32Bit(newHigh));
            } else { // n <= x (x >= n)
                long newLow = Math.max((long) starting.low, (long) eval.low);
                long newHigh = starting.high;
                update = new IntervalsWithOverflowDomain((int) wrapAround32Bit(newLow), (int) wrapAround32Bit(newHigh));
            }
        // } else if (operator instanceof ComparisonLt) {
        //     if (rightIsExpr) { // x < n
        //         long newLow = starting.low;
        //         long newHigh = Math.min((long) starting.high, (long) eval.low - 1);
        //         update = new IntervalsWithOverflowDomain((int) wrapAround32Bit(newLow), (int) wrapAround32Bit(newHigh));
        //         System.out.println("====assumeLt: starting = " + starting + ", eval = " + eval + ", update = " + update);
        //     } else { // n < x (x > n)
        //         long newLow = Math.max((long) starting.low, (long) eval.high + 1);
        //         long newHigh = starting.high;
        //         update = new IntervalsWithOverflowDomain((int) wrapAround32Bit(newLow), (int) wrapAround32Bit(newHigh));
        //         System.out.println("====assumeLt: starting = " + starting + ", eval = " + eval + ", update = " + update);
        //     }
    } else if (operator instanceof ComparisonLt) { // x < n
        if (rightIsExpr) {
            long newLow = starting.low;
            long newHigh = eval.low - 1; // Toujours eval.low - 1, pas min avec starting.high
            update = new IntervalsWithOverflowDomain((int) wrapAround32Bit(newLow), (int) wrapAround32Bit(newHigh));
            System.out.println("assumeLt: starting = " + starting + ", eval = " + eval + ", update = " + update);
        } else { // n < x (x > n)
            long newLow = eval.high + 1;
            long newHigh = starting.high;
            update = new IntervalsWithOverflowDomain((int) wrapAround32Bit(newLow), (int) wrapAround32Bit(newHigh));
            System.out.println("assumeLt: starting = " + starting + ", eval = " + eval + ", update = " + update);
        }

        } else if (operator instanceof ComparisonNe) {
            update = starting;
        } else {
            return environment;
        }

        if (update == null || update.isBottom() || update.low > update.high) {
            return environment.bottom();
        }

        IntervalsWithOverflowDomain widened = starting.wideningAux(update);
        if (widened.isBottom()) {
            return environment.bottom();
        }

        return environment.putState(id, widened);
    }


    @Override
    public StructuredRepresentation representation() {
        if (isBottom()) return new StringRepresentation("⊥");
        if (isTop()) return new StringRepresentation("T");
        return new StringRepresentation("[" + low + ", " + high + "]");
    }


    public IntervalsWithOverflowDomain add(IntervalsWithOverflowDomain left, IntervalsWithOverflowDomain right) {
        if (left.isBottom() || right.isBottom()) {
            System.out.println("add: returning bottom");
            return bottom();
        }
        long newLow = (long) left.low + (long) right.low;
        long newHigh = (long) left.high + (long) right.high;
        if (newLow >= MIN && newLow <= MAX && newHigh >= MIN && newHigh <= MAX) {
            return new IntervalsWithOverflowDomain((int) newLow, (int) newHigh);
        }
        newLow = wrapAround32Bit(newLow);
        newHigh = wrapAround32Bit(newHigh);
        return new IntervalsWithOverflowDomain((int) newLow, (int) newHigh);
    }

    public IntervalsWithOverflowDomain sub(IntervalsWithOverflowDomain left, IntervalsWithOverflowDomain right) {
        if (left.isBottom() || right.isBottom()) {
            System.out.println("sub: returning bottom");
            return bottom();
        }
        long newLow = (long) left.low - (long) right.high;
        long newHigh = (long) left.high - (long) right.low;
        System.out.println("sub: newLow = " + newLow + ", newHigh = " + newHigh);
        if (newLow >= MIN && newLow <= MAX && newHigh >= MIN && newHigh <= MAX) {
            return new IntervalsWithOverflowDomain((int) newLow, (int) newHigh);
        }
        newLow = wrapAround32Bit(newLow);
        newHigh = wrapAround32Bit(newHigh);
        return new IntervalsWithOverflowDomain((int) newLow, (int) newHigh);
    }

    public IntervalsWithOverflowDomain mul(IntervalsWithOverflowDomain left, IntervalsWithOverflowDomain right) {
        if (left.isBottom() || right.isBottom()) {
            System.out.println("mul: returning bottom");
            return bottom();
        }
        long[] results = new long[]{
                (long) left.low * right.low,
                (long) left.low * right.high,
                (long) left.high * right.low,
                (long) left.high * right.high
        };
        long min = results[0], max = results[0];
        boolean overflow = false;
        for (long r : results) {
            System.out.println("mul: result = " + r);
            if (r > MAX || r < MIN) {
                overflow = true;
                break;
            }
            min = Math.min(min, r);
            max = Math.max(max, r);
        }
        if (!overflow) {
            System.out.println("mul: min = " + min + ", max = " + max);
            return new IntervalsWithOverflowDomain((int) min, (int) max);
        }
        // Si débordement, appliquer wrap-around à tous les résultats
        for (int i = 0; i < results.length; i++) {
            results[i] = wrapAround32Bit(results[i]);
        }
        min = results[0];
        max = results[0];
        for (long r : results) {
            min = Math.min(min, r);
            max = Math.max(max, r);
        }
        System.out.println("mul: wrapped min = " + min + ", max = " + max);
        return new IntervalsWithOverflowDomain((int) min, (int) max);
    }

    public IntervalsWithOverflowDomain div(IntervalsWithOverflowDomain left, IntervalsWithOverflowDomain right) {
        if (left.isBottom() || right.isBottom()) {
            System.out.println("div: returning bottom");
            return bottom();
        }
        if (right.low <= 0 && right.high >= 0) {
            System.out.println("div: division by zero, returning bottom");
            return bottom();
        }
        long[] results = new long[]{
                (long) left.low / right.low,
                (long) left.low / right.high,
                (long) left.high / right.low,
                (long) left.high / right.high
        };
        long min = results[0], max = results[0];
        boolean overflow = false;
        for (long r : results) {
            System.out.println("div: result = " + r);
            if (r > MAX || r < MIN) {
                overflow = true;
                break;
            }
            min = Math.min(min, r);
            max = Math.max(max, r);
        }
        if (!overflow) {
            System.out.println("div: min = " + min + ", max = " + max);
            return new IntervalsWithOverflowDomain((int) min, (int) max);
        }
        // Si débordement, appliquer wrap-around à tous les résultats
        for (int i = 0; i < results.length; i++) {
            results[i] = wrapAround32Bit(results[i]);
        }
        min = results[0];
        max = results[0];
        for (long r : results) {
            min = Math.min(min, r);
            max = Math.max(max, r);
        }
        System.out.println("div: wrapped min = " + min + ", max = " + max);
        return new IntervalsWithOverflowDomain((int) min, (int) max);
    }

    private long wrapAround32Bit(long value) {
        // Ramener dans la plage [-2147483648, 2147483647] via modulo 2³²
        return ((value - MIN) % RANGE_32BIT + RANGE_32BIT) % RANGE_32BIT + MIN;
    }

}