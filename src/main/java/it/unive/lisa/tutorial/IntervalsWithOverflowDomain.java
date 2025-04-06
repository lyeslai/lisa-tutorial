package it.unive.lisa.tutorial;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
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
    private static final int[] THRESHOLDS = {
            Integer.MIN_VALUE, -1000000, -100000, -10000, -1000, -100, -10, 0, 10, 100, 1000, 10000, 100000, 1000000, Integer.MAX_VALUE
    };

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
    public IntervalsWithOverflowDomain lubAux(IntervalsWithOverflowDomain other) {
        if (this.isBottom())
            return other;
        if (other.isBottom())
            return this;
        return new IntervalsWithOverflowDomain(Math.min(this.low, other.low), Math.max(this.high, other.high));
    }

    @Override
    public IntervalsWithOverflowDomain wideningAux(IntervalsWithOverflowDomain other) {
        if (this.isBottom())
            return other;
        if (other.isBottom())
            return this;

        int newLow = this.low <= other.low ? this.low : MIN;
        int newHigh = this.high;

        if (other.high > this.high) {
            for (int t : THRESHOLDS) {
                if (t > this.high) {
                    newHigh = t;
                    break;
                }
            }
        }

        return new IntervalsWithOverflowDomain(newLow, newHigh);
    }

    @Override
    public boolean lessOrEqualAux(IntervalsWithOverflowDomain other) {
        return other.low <= this.low && this.high <= other.high;
    }

    @Override
    public IntervalsWithOverflowDomain evalNonNullConstant(Constant constant, ProgramPoint pp, SemanticOracle oracle) {
        if (constant.getValue() instanceof Integer value)
            return new IntervalsWithOverflowDomain(value, value);
        return top();
    }

    @Override
    public IntervalsWithOverflowDomain evalUnaryExpression(UnaryOperator operator, IntervalsWithOverflowDomain arg, ProgramPoint pp, SemanticOracle oracle) {
        if (arg.isBottom())
            return bottom();
        if (operator instanceof NumericNegation) {
            long newLow = -(long) arg.high;
            long newHigh = -(long) arg.low;
            return new IntervalsWithOverflowDomain((int) newLow, (int) newHigh);
        }
        return top();
    }

    @Override
    public IntervalsWithOverflowDomain evalBinaryExpression(BinaryOperator operator, IntervalsWithOverflowDomain left, IntervalsWithOverflowDomain right, ProgramPoint pp, SemanticOracle oracle) {
        if (left.isBottom() || right.isBottom())
            return bottom();
        if (operator instanceof AdditionOperator)
            return add(left, right);
        if (operator instanceof SubtractionOperator)
            return sub(left, right);
        if (operator instanceof MultiplicationOperator)
            return mul(left, right);
        if (operator instanceof DivisionOperator)
            return div(left, right);
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
        if (starting == null)
            starting = top();

        IntervalsWithOverflowDomain update = null;

        if (operator instanceof ComparisonLt) {

            if (rightIsExpr) {

                int newHigh = Math.min(starting.high, eval.low - 1);
                update = new IntervalsWithOverflowDomain(starting.low, newHigh);
            } else {

                int newLow = Math.max(starting.low, eval.low);
                update = new IntervalsWithOverflowDomain(newLow, starting.high);
            }
        } else if (operator instanceof ComparisonLe) {

            if (rightIsExpr) {
                int newHigh = Math.min(starting.high, eval.low);
                update = new IntervalsWithOverflowDomain(starting.low, newHigh);
            } else {
                int newLow = Math.max(starting.low, eval.low);
                update = new IntervalsWithOverflowDomain(newLow, starting.high);
            }
        } else if (operator instanceof ComparisonGt) {

            if (rightIsExpr) {
                int newLow = Math.max(starting.low, eval.high + 1);
                update = new IntervalsWithOverflowDomain(newLow, starting.high);
            } else {
                int newHigh = Math.min(starting.high, eval.high - 1);
                update = new IntervalsWithOverflowDomain(starting.low, newHigh);
            }
        } else if (operator instanceof ComparisonGe) {

            if (rightIsExpr) {
                int newLow = Math.max(starting.low, eval.high);
                update = new IntervalsWithOverflowDomain(newLow, starting.high);
            } else {
                int newHigh = Math.min(starting.high, eval.high);
                update = new IntervalsWithOverflowDomain(starting.low, newHigh);
            }
        } else if (operator instanceof ComparisonEq) {

            if (starting.low <= eval.low && starting.high >= eval.high) {
                update = new IntervalsWithOverflowDomain(eval.low, eval.high);
            } else {
                update = bottom();
            }
        } else if (operator instanceof ComparisonNe) {

            update = starting;
        } else {
            return environment;
        }

        if (update == null || update.isBottom()) {
            return environment;
        }

        IntervalsWithOverflowDomain refined = new IntervalsWithOverflowDomain(
                Math.max(starting.low, update.low),
                Math.min(starting.high, update.high)
        );

        if (refined.low > refined.high) {
            return environment;
        }

        ValueEnvironment<IntervalsWithOverflowDomain> newEnv = environment.putState(id, refined);

        return newEnv;
    }


    private IntervalsWithOverflowDomain add(IntervalsWithOverflowDomain l, IntervalsWithOverflowDomain r) {
        long low = (long) l.low + r.low;
        long high = (long) l.high + r.high;
        return new IntervalsWithOverflowDomain((int) wrap(low), (int) wrap(high));
    }

    private IntervalsWithOverflowDomain sub(IntervalsWithOverflowDomain l, IntervalsWithOverflowDomain r) {
        long low = (long) l.low - r.high;
        long high = (long) l.high - r.low;
        return new IntervalsWithOverflowDomain((int) wrap(low), (int) wrap(high));
    }

    private IntervalsWithOverflowDomain mul(IntervalsWithOverflowDomain l, IntervalsWithOverflowDomain r) {
        long[] results = new long[] {
                (long) l.low * r.low, (long) l.low * r.high,
                (long) l.high * r.low, (long) l.high * r.high
        };
        long min = results[0], max = results[0];
        for (long val : results) {
            min = Math.min(min, val);
            max = Math.max(max, val);
        }
        return new IntervalsWithOverflowDomain((int) wrap(min), (int) wrap(max));
    }

    private IntervalsWithOverflowDomain div(IntervalsWithOverflowDomain l, IntervalsWithOverflowDomain r) {
        if (r.low <= 0 && r.high >= 0)
            return bottom();
        long[] results = new long[] {
                (long) l.low / r.low, (long) l.low / r.high,
                (long) l.high / r.low, (long) l.high / r.high
        };
        long min = results[0], max = results[0];
        for (long val : results) {
            min = Math.min(min, val);
            max = Math.max(max, val);
        }
        return new IntervalsWithOverflowDomain((int) wrap(min), (int) wrap(max));
    }

    private long wrap(long val) {
        return ((val - MIN) % RANGE_32BIT + RANGE_32BIT) % RANGE_32BIT + MIN;
    }

    @Override
    public StructuredRepresentation representation() {
        return new StringRepresentation(toString());
    }

    @Override
    public String toString() {
        if (isBottom()) return "⊥";
        if (isTop()) return "T";
        return "[" + low + ", " + high + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof IntervalsWithOverflowDomain other)) return false;
        return low == other.low && high == other.high;
    }

    @Override
    public int hashCode() {
        return Objects.hash(low, high);
    }
}