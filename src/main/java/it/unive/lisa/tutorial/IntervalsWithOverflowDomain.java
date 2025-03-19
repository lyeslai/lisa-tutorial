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
            return true;
        }
        if (this.isBottom()) {
            return true; // ⊥ ≤ tout
        }
        if (other.isBottom()) {
            return false; // rien ≤ ⊥ sauf ⊥
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
            return new IntervalsWithOverflowDomain(value, value);
        }
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
        if (left.isBottom() || right.isBottom()) {
            return bottom();
        }
        if (operator instanceof AdditionOperator) {
            return add(right);
        } else if (operator instanceof SubtractionOperator) {
            return sub(right);
        } else if (operator instanceof MultiplicationOperator) {
            return mul(right);
        } else if (operator instanceof DivisionOperator) {
            return div(right); // Utilise la nouvelle méthode div()
        }
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

    public IntervalsWithOverflowDomain add(IntervalsWithOverflowDomain other) {
        if (this.isBottom() || other.isBottom()) {
            return bottom();
        }
        long newLow = (long) this.low + (long) other.low;
        long newHigh = (long) this.high + (long) other.high;
        if (newLow > MAX || newLow < MIN || newHigh > MAX || newHigh < MIN) {
            return top();
        }
        return new IntervalsWithOverflowDomain((int) newLow, (int) newHigh);
    }

    public IntervalsWithOverflowDomain sub(IntervalsWithOverflowDomain other) {
        if (this.isBottom() || other.isBottom()) {
            return bottom();
        }
        long newLow = (long) this.low - (long) other.high;
        long newHigh = (long) this.high - (long) other.low;
        if (newLow > MAX || newLow < MIN || newHigh > MAX || newHigh < MIN) {
            return top();
        }
        return new IntervalsWithOverflowDomain((int) newLow, (int) newHigh);
    }

    public IntervalsWithOverflowDomain mul(IntervalsWithOverflowDomain other) {
        if (this.isBottom() || other.isBottom()) {
            return bottom();
        }
        long[] results = new long[]{
                (long) this.low * other.low,
                (long) this.low * other.high,
                (long) this.high * other.low,
                (long) this.high * other.high
        };
        long min = results[0], max = results[0];
        for (long r : results) {
            if (r > MAX || r < MIN) {
                return top();
            }
            min = Math.min(min, r);
            max = Math.max(max, r);
        }
        return new IntervalsWithOverflowDomain((int) min, (int) max);
    }

    public IntervalsWithOverflowDomain div(IntervalsWithOverflowDomain other) {
        if (this.isBottom() || other.isBottom()) {
            return bottom();
        }
        if (other.low <= 0 && other.high >= 0) { // Division par zéro possible
            return bottom();
        }
        long[] results = new long[]{
                (long) this.low / other.low,
                (long) this.low / other.high,
                (long) this.high / other.low,
                (long) this.high / other.high
        };
        long min = results[0], max = results[0];
        for (long r : results) {
            if (r > MAX || r < MIN) {
                return top();
            }
            min = Math.min(min, r);
            max = Math.max(max, r);
        }
        return new IntervalsWithOverflowDomain((int) min, (int) max);
    }
}