package it.unive.lisa.tutorial;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.FunctionalLattice;
import it.unive.lisa.analysis.lattices.InverseSetLattice;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public class EqualsDomain extends FunctionalLattice<EqualsDomain, Identifier, EqualsDomain.SetOfElements> implements ValueDomain<EqualsDomain> {

    public EqualsDomain(SetOfElements lattice, Map<Identifier, SetOfElements> function) {
        super(lattice, function);
    }

    public EqualsDomain(SetOfElements lattice) {
        super(lattice);
    }

    public EqualsDomain() {
        super(new SetOfElements(Collections.emptySet(), true, null));
    }

    @Override
    public SetOfElements stateOfUnknown(Identifier identifier) {
        return new SetOfElements(Collections.singleton(identifier), true, null);
    }

    @Override
    public EqualsDomain mk(SetOfElements identifiers, Map<Identifier, SetOfElements> map) {
        return new EqualsDomain(identifiers, map);
    }

    @Override
    public EqualsDomain top() {
        return new EqualsDomain(lattice.top(), null);
    }

    @Override
    public EqualsDomain bottom() {
        return new EqualsDomain(lattice.bottom(), null);
    }

    @Override
    public EqualsDomain assign(Identifier identifier, ValueExpression valueExpression, ProgramPoint programPoint, SemanticOracle semanticOracle) throws SemanticException {
        EqualsDomain result = this.forgetIdentifier(identifier);
        
        if (valueExpression instanceof Identifier rightVariable) { // cas z = x
            Set<Identifier> value = new HashSet<>(result.getState(rightVariable).elements);
            value.add(identifier);
            Object rightConcrete = result.getState(rightVariable).getConcreteValue();
            result = result.putState(identifier, new SetOfElements(value, false, rightConcrete))
                           .putState(rightVariable, new SetOfElements(value, false, rightConcrete));
        } else if (valueExpression instanceof Constant) { // cas x = 5
            Object constantValue = ((Constant) valueExpression).getValue();
            Set<Identifier> value = new HashSet<>(Collections.singleton(identifier));
            result = result.putState(identifier, new SetOfElements(value, false, constantValue));
        } else if (valueExpression instanceof BinaryExpression binary) { // cas k = y + 1, k = y - 1, etc.
            ValueExpression left = (ValueExpression) binary.getLeft();
            ValueExpression right = (ValueExpression) binary.getRight();
            Set<Identifier> value = new HashSet<>(Collections.singleton(identifier));
            Object operator = binary.getOperator();

            if (left instanceof Constant && right instanceof Constant) {
                Object leftValue = ((Constant) left).getValue();
                Object rightValue = ((Constant) right).getValue();
                if (leftValue instanceof Number && rightValue instanceof Number) {
                    Number resultValue = computeBinaryOperation(operator, (Number) leftValue, (Number) rightValue);
                    if (resultValue != null) {
                        result = result.putState(identifier, new SetOfElements(value, false, resultValue));
                    } else {
                        result = result.putState(identifier, new SetOfElements(value, false, null));
                    }
                } else {
                    result = result.putState(identifier, new SetOfElements(value, false, null));
                }
            } else if (left instanceof Identifier leftVar && right instanceof Constant) {
                Object rightValue = ((Constant) right).getValue();
                Object leftConcrete = result.getState(leftVar).getConcreteValue();
                if (leftConcrete instanceof Number && rightValue instanceof Number) {
                    Number resultValue = computeBinaryOperation(operator, (Number) leftConcrete, (Number) rightValue);
                    if (resultValue != null) {
                        result = result.putState(identifier, new SetOfElements(value, false, resultValue));
                    } else {
                        result = result.putState(identifier, new SetOfElements(value, false, null));
                    }
                } else {
                    result = result.putState(identifier, new SetOfElements(value, false, null));
                }
            } else if (left instanceof Constant && right instanceof Identifier rightVar) {
                Object leftValue = ((Constant) left).getValue();
                Object rightConcrete = result.getState(rightVar).getConcreteValue();
                if (leftValue instanceof Number && rightConcrete instanceof Number) {
                    Number resultValue = computeBinaryOperation(operator, (Number) leftValue, (Number) rightConcrete);
                    if (resultValue != null) {
                        result = result.putState(identifier, new SetOfElements(value, false, resultValue));
                    } else {
                        result = result.putState(identifier, new SetOfElements(value, false, null));
                    }
                } else {
                    result = result.putState(identifier, new SetOfElements(value, false, null));
                }
            } else if (left instanceof Identifier leftVar && right instanceof Identifier rightVar) {
                Object leftConcrete = result.getState(leftVar).getConcreteValue();
                Object rightConcrete = result.getState(rightVar).getConcreteValue();
                if (leftConcrete instanceof Number && rightConcrete instanceof Number) {
                    Number resultValue = computeBinaryOperation(operator, (Number) leftConcrete, (Number) rightConcrete);
                    if (resultValue != null) {
                        result = result.putState(identifier, new SetOfElements(value, false, resultValue));
                    } else {
                        result = result.putState(identifier, new SetOfElements(value, false, null));
                    }
                } else {
                    result = result.putState(identifier, new SetOfElements(value, false, null));
                }
            } else {
                result = result.putState(identifier, new SetOfElements(value, false, null));
            }
        }
        return result.close(); 
    }

    private Number computeBinaryOperation(Object operator, Number left, Number right) {
        if (operator instanceof it.unive.lisa.symbolic.value.operator.binary.BinaryOperator) {
            String opStr = operator.toString();
            return switch (opStr) {
                case "+" -> left.intValue() + right.intValue();
                case "-" -> left.intValue() - right.intValue();
                case "*" -> left.intValue() * right.intValue();
                case "/" -> {
                    if (right.intValue() != 0) {
                        yield left.intValue() / right.intValue();
                    }
                    yield null;
                }
                case "%" -> {
                    if (right.intValue() != 0) {
                        yield left.intValue() % right.intValue();
                    }
                    yield null;
                }
                default -> null; // Opérateur non géré
            };
        }
        return null; // Opérateur inconnu
    }

    @Override
    public EqualsDomain assume(ValueExpression valueExpression, ProgramPoint programPoint, ProgramPoint programPoint1, SemanticOracle semanticOracle) throws SemanticException {
        EqualsDomain result = this;
        if (valueExpression instanceof BinaryExpression binaryExpression) {
            if (binaryExpression.getOperator() instanceof ComparisonEq) {
                if (binaryExpression.getLeft() instanceof Identifier leftVariable && binaryExpression.getRight() instanceof Identifier rightVariable) { // cas x == y
                    Set<Identifier> value = new HashSet<>(result.getState(leftVariable).elements);
                    value.add(rightVariable);
                    value.addAll(result.getState(rightVariable).elements);
                    Object leftConcrete = result.getState(leftVariable).getConcreteValue();
                    Object rightConcrete = result.getState(rightVariable).getConcreteValue();
                    Object concrete = (leftConcrete != null) ? leftConcrete : rightConcrete;
                    if (leftConcrete != null && rightConcrete != null && !leftConcrete.equals(rightConcrete)) {
                        return result.bottom();
                    }
                    result = result.putState(leftVariable, new SetOfElements(value, false, concrete))
                                   .putState(rightVariable, new SetOfElements(value, false, concrete));
                } else if (binaryExpression.getLeft() instanceof Identifier leftVariable && binaryExpression.getRight() instanceof Constant) { // cas x == 5
                    Object constantValue = ((Constant) binaryExpression.getRight()).getValue();
                    Set<Identifier> currentSet = result.getState(leftVariable).elements;
                    Object currentConcrete = result.getState(leftVariable).getConcreteValue();
                    if (currentConcrete != null && !currentConcrete.equals(constantValue)) {
                        return result.bottom();
                    }
                    result = result.putState(leftVariable, new SetOfElements(currentSet, false, constantValue));
                } else if (binaryExpression.getLeft() instanceof Identifier leftVariable && binaryExpression.getRight() instanceof BinaryExpression) { // cas x == y + 1
                    Set<Identifier> currentSet = result.getState(leftVariable).elements;
                    if (currentSet.size() > 1 || result.getState(leftVariable).getConcreteValue() != null) {
                        return result.bottom();
                    }
                }
            }
        }
        return result.close(); // Fusion globale avec close()
    }

    private EqualsDomain close() {
        EqualsDomain result = this;
        Map<Object, Set<Identifier>> valueGroups = new HashMap<>();
    
        //Regrouper par valeurs concrètes uniquement
        for (Identifier id : this.getKeys()) {
            Object concrete = result.getState(id).getConcreteValue();
            if (concrete != null) {
                valueGroups.computeIfAbsent(concrete, k -> new HashSet<>()).add(id);
            }
        }
    
        //Fusionner uniquement sur égalités explicites ou valeurs concrètes
        for (Identifier id1 : this.getKeys()) {
            Set<Identifier> eqSet = new HashSet<>(result.getState(id1).elements);
            Object concrete = result.getState(id1).getConcreteValue();
    
            if (concrete != null && valueGroups.containsKey(concrete)) {
                eqSet.addAll(valueGroups.get(concrete));
            }
    
            for (Identifier id : eqSet) {
                result = result.putState(id, new SetOfElements(eqSet, false, concrete));
            }
        }
        return result;
    }

    @Override
    public boolean knowsIdentifier(Identifier identifier) {
        return this.getKeys().contains(identifier);
    }

    @Override
    public EqualsDomain forgetIdentifier(Identifier identifier) throws SemanticException {
        EqualsDomain result = this;
        if (result.getKeys().contains(identifier)) {
            result = result.putState(identifier, new SetOfElements(Collections.singleton(identifier), true, null));
        }
        return result;
    }

    @Override
    public EqualsDomain forgetIdentifiersIf(Predicate<Identifier> predicate) throws SemanticException {
        return this;
    }

    @Override
    public Satisfiability satisfies(ValueExpression valueExpression, ProgramPoint programPoint, SemanticOracle semanticOracle) throws SemanticException {
        if (valueExpression instanceof BinaryExpression binaryExpression) {
            if (binaryExpression.getOperator() instanceof ComparisonEq) {
                if (binaryExpression.getLeft() instanceof Identifier left && binaryExpression.getRight() instanceof Identifier right) { // y == x
                    Set<Identifier> leftSet = this.getState(left).elements;
                    Object leftConcrete = this.getState(left).getConcreteValue();
                    Object rightConcrete = this.getState(right).getConcreteValue();
                    if (leftSet.contains(right) || (leftConcrete != null && leftConcrete.equals(rightConcrete))) {
                        return Satisfiability.SATISFIED;
                    }
                } else if (binaryExpression.getLeft() instanceof Identifier left && binaryExpression.getRight() instanceof Constant) { // x == 5
                    Object constantValue = ((Constant) binaryExpression.getRight()).getValue();
                    Object leftConcrete = this.getState(left).getConcreteValue();
                    if (leftConcrete != null && leftConcrete.equals(constantValue)) {
                        return Satisfiability.SATISFIED;
                    }
                }
            }
        }
        return Satisfiability.UNKNOWN;
    }

    @Override
    public EqualsDomain smallStepSemantics(ValueExpression valueExpression, ProgramPoint programPoint, SemanticOracle semanticOracle) throws SemanticException {
        return this;
    }

    @Override
    public EqualsDomain pushScope(ScopeToken scopeToken) throws SemanticException {
        return this;
    }

    @Override
    public EqualsDomain popScope(ScopeToken scopeToken) throws SemanticException {
        return this;
    }

    public static class SetOfElements extends InverseSetLattice<SetOfElements, Identifier> {
        private final Set<Identifier> elements;
        private final boolean isTop;
        private final Object concreteValue;

        public SetOfElements(Set<Identifier> elements, boolean isTop, Object concreteValue) {
            super(elements, isTop);
            this.elements = elements;
            this.isTop = isTop;
            this.concreteValue = concreteValue;
        }

        public SetOfElements(Set<Identifier> elements, boolean isTop) {
            this(elements, isTop, null);
        }

        @Override
        public SetOfElements mk(Set<Identifier> set) {
            return new SetOfElements(set, set.isEmpty(), null);
        }

        @Override
        public SetOfElements top() {
            return new SetOfElements(Collections.emptySet(), true, null);
        }

        @Override
        public SetOfElements bottom() {
            return new SetOfElements(Collections.emptySet(), false, null);
        }

        public Object getConcreteValue() {
            return concreteValue;
        }

        public SetOfElements withConcreteValue(Object newValue) {
            return new SetOfElements(this.elements, this.isTop, newValue);
        }
    }
}