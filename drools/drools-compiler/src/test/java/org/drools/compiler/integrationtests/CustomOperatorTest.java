package org.drools.compiler.integrationtests;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.drools.compiler.CommonTestMethodBase;
import org.drools.core.base.BaseEvaluator;
import org.drools.core.base.ValueType;
import org.drools.core.base.evaluators.EvaluatorDefinition;
import org.drools.core.base.evaluators.Operator;
import org.drools.core.common.InternalFactHandle;
import org.drools.core.common.InternalWorkingMemory;
import org.drools.rule.VariableRestriction.ObjectVariableContextEntry;
import org.drools.rule.VariableRestriction.VariableContextEntry;
import org.drools.core.spi.Evaluator;
import org.drools.core.spi.FieldValue;
import org.drools.core.spi.InternalReadAccessor;
import org.junit.Test;
import org.kie.KnowledgeBase;
import org.kie.builder.KnowledgeBuilderConfiguration;
import org.kie.builder.KnowledgeBuilderFactory;
import org.kie.builder.conf.EvaluatorOption;

public class CustomOperatorTest extends CommonTestMethodBase {

    @Test
    public void testCustomOperatorCombiningConstraints() {
        // JBRULES-3517
        String str =
                "declare GN\n" +
                "   gNo : Double\n" +
                "end\n" +
                "\n" +
                "declare t547147\n" +
                "   c547148 : String\n" +
                "   c547149 : String\n" +
                "end\n" +
                "\n" +
                "declare Tra48\n" +
                "   gNo : Double\n" +
                "   postCode : String\n" +
                "   name : String\n" +
                "   cnt : String\n" +
                "end\n" +
                "\n" +
                "rule \"r548695.1\"\n" +
                "no-loop true\n" +
                "dialect \"mvel\"\n" +
                "when\n" +
                "   gnId : GN()\n" +
                "   la : t547147( )\n" +
                "   v1717 : Tra48( gnId.gNo == gNo, name F_str[startsWith] la.c547148 || postCode F_str[contains] la.c547149 )\n" +
                "then\n" +
                "   System.out.println(\"Rule r548695.1 fired\");\n" +
                "end\n";

        KnowledgeBuilderConfiguration builderConf = KnowledgeBuilderFactory.newKnowledgeBuilderConfiguration();
        builderConf.setOption(EvaluatorOption.get("str", new F_StrEvaluatorDefinition()));

        KnowledgeBase kbase = loadKnowledgeBaseFromString(builderConf, str);
    }

    public static class F_StrEvaluatorDefinition implements EvaluatorDefinition {

        public static final Operator STR_COMPARE = Operator.addOperatorToRegistry("F_str", false);
        public static final Operator NOT_STR_COMPARE = Operator.addOperatorToRegistry("F_str", true);
        private static final String[] SUPPORTED_IDS = {STR_COMPARE.getOperatorString()};

        public enum Operations {

            startsWith, endsWith, length, contains, bidicontains;
        }

        private Evaluator[] evaluator;

        public String[] getEvaluatorIds() {
            return F_StrEvaluatorDefinition.SUPPORTED_IDS;
        }

        public boolean isNegatable() {
            return true;
        }

        public Evaluator getEvaluator(ValueType type, String operatorId, boolean isNegated, String parameterText, Target leftTarget, Target rightTarget) {
            F_StrEvaluator evaluatorLocal = new F_StrEvaluator(type, isNegated);
            evaluatorLocal.setParameterText(parameterText);
            return evaluatorLocal;
        }

        public Evaluator getEvaluator(ValueType type, String operatorId, boolean isNegated, String parameterText) {
            return getEvaluator(type, operatorId, isNegated, parameterText, Target.FACT, Target.FACT);
        }

        public Evaluator getEvaluator(ValueType type, Operator operator, String parameterText) {
            return this.getEvaluator(type, operator.getOperatorString(), operator.isNegated(), parameterText);
        }

        public Evaluator getEvaluator(ValueType type, Operator operator) {
            return this.getEvaluator(type, operator.getOperatorString(), operator.isNegated(), null);
        }

        public boolean supportsType(ValueType vt) {
            return true;
        }

        public Target getTarget() {
            return Target.FACT;
        }

        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeObject(evaluator);
        }

        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            evaluator = (Evaluator[]) in.readObject();
        }
    }

    public static class F_StrEvaluator extends BaseEvaluator {

        private F_StrEvaluatorDefinition.Operations parameter;

        public void setParameterText(String parameterText) {
            this.parameter = F_StrEvaluatorDefinition.Operations.valueOf(parameterText);
        }

        public F_StrEvaluatorDefinition.Operations getParameter() {
            return parameter;
        }

        public F_StrEvaluator(final ValueType type, final boolean isNegated) {
            super(type, isNegated ? F_StrEvaluatorDefinition.NOT_STR_COMPARE : F_StrEvaluatorDefinition.STR_COMPARE);
        }

        public boolean evaluate(InternalWorkingMemory workingMemory, InternalReadAccessor extractor, InternalFactHandle factHandle, FieldValue value) {
            final Object objectValue = extractor.getValue(workingMemory, factHandle);
            String objectValueString = (String) objectValue;
            return evaluateAll((String) value.getValue(), objectValueString);
        }

        public boolean evaluate(InternalWorkingMemory iwm, InternalReadAccessor ira, InternalFactHandle left, InternalReadAccessor ira1, InternalFactHandle right) {
            return evaluateAll((String) left.getObject(), (String) right.getObject());
        }

        public boolean evaluateCachedLeft(InternalWorkingMemory workingMemory, VariableContextEntry context, InternalFactHandle right) {
            final Object valRight = context.extractor.getValue(workingMemory, right);
            return evaluateAll((String) ((ObjectVariableContextEntry) context).left, (String) valRight);
        }

        public boolean evaluateCachedRight(InternalWorkingMemory workingMemory, VariableContextEntry context, InternalFactHandle left) {
            final Object varLeft = context.declaration.getExtractor().getValue(workingMemory, left);
            return evaluateAll((String) varLeft, (String) ((ObjectVariableContextEntry) context).right);
        }

        public boolean evaluateAll(String leftString, String rightString) {
            boolean result = ((leftString != null) && (rightString != null));

            if (result) {
                switch (parameter) {
                    case startsWith:
                        result = this.getOperator().isNegated() ^ (leftString.startsWith(rightString));
                        return result;
                    case endsWith:
                        result = this.getOperator().isNegated() ^ (leftString.endsWith(rightString));
                        return result;
                }
            }
            return result;
        }
    }
}
