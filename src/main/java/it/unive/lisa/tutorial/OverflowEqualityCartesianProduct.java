package it.unive.lisa.tutorial;

import it.unive.lisa.analysis.SemanticDomain;
import it.unive.lisa.analysis.combination.CartesianProduct;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;

public class OverflowEqualityCartesianProduct extends CartesianProduct<OverflowEqualityCartesianProduct,EqualsDomain, ValueEnvironment<IntervalsWithOverflowDomain>,ValueExpression,Identifier>
    implements ValueDomain<OverflowEqualityCartesianProduct> {

    /**
     * Builds the Cartesian product abstract domain.
     *
     * @param left  the left-hand side of the Cartesian product
     * @param right the right-hand side of the Cartesian product
     */
    public OverflowEqualityCartesianProduct(EqualsDomain left, ValueEnvironment<IntervalsWithOverflowDomain> right) {
        super(left, right);
    }

    @Override
    public OverflowEqualityCartesianProduct mk(EqualsDomain left, ValueEnvironment<IntervalsWithOverflowDomain> right) {
        return new OverflowEqualityCartesianProduct(left, right);
    }

    @Override
    public boolean knowsIdentifier(Identifier id) {
        return left.knowsIdentifier(id);
    }
}
