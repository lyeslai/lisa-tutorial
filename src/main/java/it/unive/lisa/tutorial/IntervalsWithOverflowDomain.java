package it.unive.lisa.tutorial;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.operator.AdditionOperator;
import it.unive.lisa.symbolic.value.operator.DivisionOperator;
import it.unive.lisa.symbolic.value.operator.MultiplicationOperator;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonNe;
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

        final int THRESHOLD = 1000;
        int newLow = this.low;
        int newHigh = this.high;

        if (other.low < this.low) {
            int diff = this.low - other.low;
            if (diff > THRESHOLD && this.low > MIN + THRESHOLD) {
                newLow = this.low - THRESHOLD;
            } else if (diff > 0) {
                newLow = MIN;
            }
        }

        if (other.high > this.high) {
            int diff = other.high - this.high;
            if (diff > THRESHOLD && this.high < MAX - THRESHOLD) {
                newHigh = this.high + THRESHOLD;
            } else if (diff > 0) {
                newHigh = MAX;
            }
        }

        return new IntervalsWithOverflowDomain(newLow, newHigh);
    }


    @Override
    public boolean lessOrEqualAux(IntervalsWithOverflowDomain other) throws SemanticException {
        if (this.isBottom() && other.isBottom()) {
            return true; // ⊥ ≤ ⊥
        }
        if (this.isBottom() || other.isBottom()) {
            return false; // Si un seul est ⊥, pas évaluable, donc pas ≤
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

    public Satisfiability satisfiesBinaryExpression(BinaryOperator operator, IntervalsWithOverflowDomain left, IntervalsWithOverflowDomain right) {
        if (left.isBottom() || right.isBottom()) {
            return Satisfiability.BOTTOM;
        }
        if (operator instanceof ComparisonLt) { // <
            if (left.high < right.low) return Satisfiability.SATISFIED;
            if (left.low >= right.high) return Satisfiability.NOT_SATISFIED;
            return Satisfiability.UNKNOWN;
        } else if (operator instanceof ComparisonGt) { // >
            if (left.low > right.high) return Satisfiability.SATISFIED;
            if (left.high <= right.low) return Satisfiability.NOT_SATISFIED;
            return Satisfiability.UNKNOWN;
        } else if (operator instanceof ComparisonEq) { // ==
            if (left.high < right.low || left.low > right.high) return Satisfiability.NOT_SATISFIED;
            if (left.low == left.high && right.low == right.high && left.low == right.low) return Satisfiability.SATISFIED;
            return Satisfiability.UNKNOWN;
        } else if (operator instanceof ComparisonNe) { // !=
            if (left.high < right.low || left.low > right.high) return Satisfiability.SATISFIED;
            if (left.low == left.high && right.low == right.high && left.low == right.low) return Satisfiability.NOT_SATISFIED;
            return Satisfiability.UNKNOWN;
        }
        return Satisfiability.UNKNOWN;
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