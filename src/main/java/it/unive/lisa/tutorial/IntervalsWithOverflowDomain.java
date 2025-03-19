package it.unive.lisa.tutorial;

import it.unive.lisa.analysis.nonrelational.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.lattices.Satisfiability;

public class IntervalsWithOverflowDomain extends BaseNonRelationalValueDomain<IntervalsWithOverflowDomain> {

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
    protected IntervalsWithOverflowDomain lubAux(IntervalsWithOverflowDomain other) {
        return new IntervalsWithOverflowDomain(
            Math.min(this.low, other.low),
            Math.max(this.high, other.high)
        );
    }

    @Override
    protected IntervalsWithOverflowDomain wideningAux(IntervalsWithOverflowDomain other) {
        final int THRESHOLD = 1000; // Ajustable selon LiSA
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
    protected boolean lessOrEqualAux(IntervalsWithOverflowDomain other) {
        return other.low <= this.low && this.high <= other.high;
    }

    @Override
    public Satisfiability satisfiesBinaryExpression(BinaryOperator operator, IntervalsWithOverflowDomain left, IntervalsWithOverflowDomain right) {
        switch (operator) {
            case LESS_THAN:
                if (left.high < right.low) return Satisfiability.SATISFIED;
                if (left.low >= right.high) return Satisfiability.NOT_SATISFIED;
                return Satisfiability.UNKNOWN;

            case GREATER_THAN:
                if (left.low > right.high) return Satisfiability.SATISFIED;
                if (left.high <= right.low) return Satisfiability.NOT_SATISFIED;
                return Satisfiability.UNKNOWN;

            case EQUAL:
                if (left.high < right.low || left.low > right.high) return Satisfiability.NOT_SATISFIED;
                if (left.low == left.high && right.low == right.high && left.low == right.low) return Satisfiability.SATISFIED;
                return Satisfiability.UNKNOWN;

            case NOT_EQUAL:
                if (left.high < right.low || left.low > right.high) return Satisfiability.SATISFIED;
                if (left.low == left.high && right.low == right.high && left.low == right.low) return Satisfiability.NOT_SATISFIED;
                return Satisfiability.UNKNOWN;

            default:
                return Satisfiability.UNKNOWN;
        }
    }

    @Override
    public String representation() {
        if (isBottom()) return "⊥";
        if (isTop()) return "T";
        return "[" + low + ", " + high + "]";
    }

    public IntervalsWithOverflowDomain add(IntervalsWithOverflowDomain other) {
        long newLow = (long) this.low + (long) other.low;
        long newHigh = (long) this.high + (long) other.high;
        if (newLow > MAX || newLow < MIN || newHigh > MAX || newHigh < MIN) {
            return top();
        }
        return new IntervalsWithOverflowDomain((int) newLow, (int) newHigh);
    }

    public IntervalsWithOverflowDomain sub(IntervalsWithOverflowDomain other) {
        long newLow = (long) this.low - (long) other.high;
        long newHigh = (long) this.high - (long) other.low;
        if (newLow > MAX || newLow < MIN || newHigh > MAX || newHigh < MIN) {
            return top();
        }
        return new IntervalsWithOverflowDomain((int) newLow, (int) newHigh);
    }

    public IntervalsWithOverflowDomain mul(IntervalsWithOverflowDomain other) {
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
}