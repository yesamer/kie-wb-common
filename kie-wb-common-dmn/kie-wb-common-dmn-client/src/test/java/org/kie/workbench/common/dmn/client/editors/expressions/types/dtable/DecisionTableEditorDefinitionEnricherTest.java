/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.workbench.common.dmn.client.editors.expressions.types.dtable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.ait.lienzo.test.LienzoMockitoTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.workbench.common.dmn.api.definition.model.Context;
import org.kie.workbench.common.dmn.api.definition.model.ContextEntry;
import org.kie.workbench.common.dmn.api.definition.model.DMNDiagram;
import org.kie.workbench.common.dmn.api.definition.model.DecisionTable;
import org.kie.workbench.common.dmn.api.definition.model.Definitions;
import org.kie.workbench.common.dmn.api.definition.model.InformationItem;
import org.kie.workbench.common.dmn.api.definition.model.InformationItemPrimary;
import org.kie.workbench.common.dmn.api.definition.model.InputClause;
import org.kie.workbench.common.dmn.api.definition.model.InputClauseLiteralExpression;
import org.kie.workbench.common.dmn.api.definition.model.InputData;
import org.kie.workbench.common.dmn.api.definition.model.ItemDefinition;
import org.kie.workbench.common.dmn.api.definition.model.OutputClause;
import org.kie.workbench.common.dmn.api.property.dmn.Description;
import org.kie.workbench.common.dmn.api.property.dmn.Id;
import org.kie.workbench.common.dmn.api.property.dmn.Name;
import org.kie.workbench.common.dmn.api.property.dmn.QName;
import org.kie.workbench.common.dmn.api.property.dmn.types.BuiltInType;
import org.kie.workbench.common.dmn.client.editors.types.common.ItemDefinitionUtils;
import org.kie.workbench.common.dmn.client.graph.DMNGraphUtils;
import org.kie.workbench.common.stunner.core.graph.Edge;
import org.kie.workbench.common.stunner.core.graph.Node;
import org.kie.workbench.common.stunner.core.graph.content.definition.Definition;
import org.kie.workbench.common.stunner.core.graph.content.definition.DefinitionImpl;
import org.kie.workbench.common.stunner.core.graph.impl.EdgeImpl;
import org.kie.workbench.common.stunner.core.graph.impl.NodeImpl;
import org.kie.workbench.common.stunner.core.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.kie.workbench.common.dmn.api.property.dmn.types.BuiltInType.NUMBER;
import static org.kie.workbench.common.dmn.api.property.dmn.types.BuiltInType.STRING;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(LienzoMockitoTestRunner.class)
public class DecisionTableEditorDefinitionEnricherTest extends BaseDecisionTableEditorDefinitionTest {

    private static final String NODE_UUID = UUID.uuid();

    private static final String INPUT_DATA_NAME_1 = "z-inputData1";

    private static final String INPUT_DATA_NAME_2 = "a-inputData2";

    private static final QName INPUT_DATA_QNAME_1 = STRING.asQName();

    private static final QName INPUT_DATA_QNAME_2 = NUMBER.asQName();

    private static final QName OUTPUT_DATA_QNAME = BuiltInType.DATE.asQName();

    private DMNDiagram diagram;

    private InputData inputData1;

    private InputData inputData2;

    @Before
    public void setup() {
        super.setup();

        this.diagram = new DMNDiagram();
        this.inputData1 = new InputData();
        this.inputData2 = new InputData();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testModelEnrichmentWhenTopLevelDecisionTableWithInputData() {
        setupGraph();

        final Optional<DecisionTable> oModel = definition.getModelClass();
        definition.enrich(Optional.of(NODE_UUID), decision, oModel);

        final DecisionTable model = oModel.get();
        assertBasicEnrichment(model);

        final List<InputClause> input = model.getInput();
        assertThat(input.size()).isEqualTo(2);
        assertThat(input.get(0).getInputExpression()).isInstanceOf(InputClauseLiteralExpression.class);
        assertThat(input.get(0).getInputExpression().getText().getValue()).isEqualTo(INPUT_DATA_NAME_2);
        assertThat(input.get(0).getInputExpression().getTypeRef()).isEqualTo(INPUT_DATA_QNAME_2);
        assertThat(input.get(1).getInputExpression()).isInstanceOf(InputClauseLiteralExpression.class);
        assertThat(input.get(1).getInputExpression().getText().getValue()).isEqualTo(INPUT_DATA_NAME_1);
        assertThat(input.get(1).getInputExpression().getTypeRef()).isEqualTo(INPUT_DATA_QNAME_1);

        assertStandardOutputClauseEnrichment(model);
        assertStandardDecisionRuleEnrichment(model, 2, 1);
        assertParentHierarchyEnrichment(model, 2, 1);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testModelEnrichmentWhenTopLevelDecisionTableWithInputDataAndSimpleCustomType() {
        setupGraph();

        final Definitions definitions = diagram.getDefinitions();

        final String simpleItemDefinitionName = "tSmurf";
        final QName simpleItemDefinitionTypeRef = new QName(QName.NULL_NS_URI, BuiltInType.DATE.getName());
        final ItemDefinition simpleItemDefinition = new ItemDefinition();
        simpleItemDefinition.setName(new Name(simpleItemDefinitionName));
        simpleItemDefinition.setTypeRef(simpleItemDefinitionTypeRef);
        definitions.getItemDefinition().add(simpleItemDefinition);

        final QName inputData1TypeRef = new QName(QName.NULL_NS_URI, simpleItemDefinitionName);
        inputData1.getVariable().setTypeRef(inputData1TypeRef);

        final Optional<DecisionTable> oModel = definition.getModelClass();
        definition.enrich(Optional.of(NODE_UUID), decision, oModel);

        final DecisionTable model = oModel.get();
        assertBasicEnrichment(model);

        final List<InputClause> input = model.getInput();
        assertThat(input.size()).isEqualTo(2);
        assertThat(input.get(0).getInputExpression()).isInstanceOf(InputClauseLiteralExpression.class);
        assertThat(input.get(0).getInputExpression().getText().getValue()).isEqualTo(INPUT_DATA_NAME_2);
        assertThat(input.get(0).getInputExpression().getTypeRef()).isEqualTo(INPUT_DATA_QNAME_2);
        assertThat(input.get(1).getInputExpression()).isInstanceOf(InputClauseLiteralExpression.class);
        assertThat(input.get(1).getInputExpression().getText().getValue()).isEqualTo(INPUT_DATA_NAME_1);
        assertThat(input.get(1).getInputExpression().getTypeRef()).isEqualTo(simpleItemDefinitionTypeRef);

        assertStandardOutputClauseEnrichment(model);
        assertStandardDecisionRuleEnrichment(model, 2, 1);
        assertParentHierarchyEnrichment(model, 2, 1);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testModelEnrichmentWhenTopLevelDecisionTableWithInputDataAndComplexCustomType() {
        setupGraph();

        final Definitions definitions = diagram.getDefinitions();

        final String complexItemDefinitionName = "tSmurf";
        final String complexItemDefinitionPart1Name = "tDateOfBirth";
        final String complexItemDefinitionPart2Name = "tIsBlue";
        final QName complexItemDefinitionPart1TypeRef = new QName(QName.NULL_NS_URI, BuiltInType.DATE.getName());
        final QName complexItemDefinitionPart2TypeRef = new QName(QName.NULL_NS_URI, BuiltInType.BOOLEAN.getName());
        final ItemDefinition complexItemDefinition = new ItemDefinition();
        complexItemDefinition.setName(new Name(complexItemDefinitionName));
        complexItemDefinition.getItemComponent().add(new ItemDefinition() {{
            setName(new Name(complexItemDefinitionPart1Name));
            setTypeRef(complexItemDefinitionPart1TypeRef);
        }});
        complexItemDefinition.getItemComponent().add(new ItemDefinition() {{
            setName(new Name(complexItemDefinitionPart2Name));
            setTypeRef(complexItemDefinitionPart2TypeRef);
        }});

        definitions.getItemDefinition().add(complexItemDefinition);

        final QName inputData1TypeRef = new QName(QName.NULL_NS_URI, complexItemDefinitionName);
        inputData1.getVariable().setTypeRef(inputData1TypeRef);

        final Optional<DecisionTable> oModel = definition.getModelClass();
        definition.enrich(Optional.of(NODE_UUID), decision, oModel);

        final DecisionTable model = oModel.get();
        assertBasicEnrichment(model);

        final List<InputClause> input = model.getInput();
        assertThat(input.size()).isEqualTo(3);
        assertThat(input.get(0).getInputExpression()).isInstanceOf(InputClauseLiteralExpression.class);
        assertThat(input.get(0).getInputExpression().getText().getValue()).isEqualTo(INPUT_DATA_NAME_2);
        assertThat(input.get(0).getInputExpression().getTypeRef()).isEqualTo(INPUT_DATA_QNAME_2);
        assertThat(input.get(1).getInputExpression()).isInstanceOf(InputClauseLiteralExpression.class);
        assertThat(input.get(1).getInputExpression().getText().getValue()).isEqualTo(INPUT_DATA_NAME_1 + "." + complexItemDefinitionPart1Name);
        assertThat(input.get(1).getInputExpression().getTypeRef()).isEqualTo(complexItemDefinitionPart1TypeRef);
        assertThat(input.get(2).getInputExpression()).isInstanceOf(InputClauseLiteralExpression.class);
        assertThat(input.get(2).getInputExpression().getText().getValue()).isEqualTo(INPUT_DATA_NAME_1 + "." + complexItemDefinitionPart2Name);
        assertThat(input.get(2).getInputExpression().getTypeRef()).isEqualTo(complexItemDefinitionPart2TypeRef);

        assertStandardOutputClauseEnrichment(model);
        assertStandardDecisionRuleEnrichment(model, 3, 1);
        assertParentHierarchyEnrichment(model, 3, 1);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testModelEnrichmentWhenTopLevelDecisionTableWithInputDataAndRecursiveCustomType() {
        setupGraph();

        final Definitions definitions = diagram.getDefinitions();

        final String tSmurfName = "tSmurf";
        final String tDateOfBirthName = "tDateOfBirth";
        final String tIsBlueName = "tIsBlue";
        final String tParentName = "tParent";
        final QName dateBuiltInType = new QName(QName.NULL_NS_URI, BuiltInType.DATE.getName());
        final QName booleanBuiltInType = new QName(QName.NULL_NS_URI, BuiltInType.BOOLEAN.getName());
        final QName parentCustomType = new QName(QName.NULL_NS_URI, tSmurfName);
        final ItemDefinition tSmurfCustomDataType = new ItemDefinition();
        tSmurfCustomDataType.setName(new Name(tSmurfName));
        tSmurfCustomDataType.getItemComponent().add(new ItemDefinition() {{
            setName(new Name(tDateOfBirthName));
            setTypeRef(dateBuiltInType);
        }});
        tSmurfCustomDataType.getItemComponent().add(new ItemDefinition() {{
            setName(new Name(tIsBlueName));
            setTypeRef(booleanBuiltInType);
        }});
        tSmurfCustomDataType.getItemComponent().add(new ItemDefinition() {{
            setName(new Name(tParentName));
            setTypeRef(parentCustomType);
        }});

        definitions.getItemDefinition().add(tSmurfCustomDataType);

        final QName inputData1TypeRef = new QName(QName.NULL_NS_URI, tSmurfName);
        inputData1.getVariable().setTypeRef(inputData1TypeRef);

        final Optional<DecisionTable> oModel = definition.getModelClass();
        definition.enrich(Optional.of(NODE_UUID), decision, oModel);

        final DecisionTable model = oModel.get();
        assertBasicEnrichment(model);

        final List<InputClause> input = model.getInput();
        assertThat(input.size()).isEqualTo(4);
        assertThat(input.get(0).getInputExpression()).isInstanceOf(InputClauseLiteralExpression.class);
        assertThat(input.get(0).getInputExpression().getText().getValue()).isEqualTo(INPUT_DATA_NAME_2);
        assertThat(input.get(0).getInputExpression().getTypeRef()).isEqualTo(INPUT_DATA_QNAME_2);
        assertThat(input.get(1).getInputExpression()).isInstanceOf(InputClauseLiteralExpression.class);
        assertThat(input.get(1).getInputExpression().getText().getValue()).isEqualTo(INPUT_DATA_NAME_1 + "." + tDateOfBirthName);
        assertThat(input.get(1).getInputExpression().getTypeRef()).isEqualTo(dateBuiltInType);
        assertThat(input.get(2).getInputExpression()).isInstanceOf(InputClauseLiteralExpression.class);
        assertThat(input.get(2).getInputExpression().getText().getValue()).isEqualTo(INPUT_DATA_NAME_1 + "." + tIsBlueName);
        assertThat(input.get(2).getInputExpression().getTypeRef()).isEqualTo(booleanBuiltInType);
        assertThat(input.get(3).getInputExpression()).isInstanceOf(InputClauseLiteralExpression.class);
        assertThat(input.get(3).getInputExpression().getText().getValue()).isEqualTo(INPUT_DATA_NAME_1 + "." + tParentName);
        assertThat(input.get(3).getInputExpression().getTypeRef()).isEqualTo(parentCustomType);

        assertStandardOutputClauseEnrichment(model);
        assertStandardDecisionRuleEnrichment(model, 4, 1);
        assertParentHierarchyEnrichment(model, 4, 1);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testModelEnrichmentWhenTopLevelDecisionTableWithMultipleHierarchyCustomTypes() {
        setupGraph();

        final Definitions definitions = diagram.getDefinitions();

        final String tSmurf = "tSmurf";
        final String tSmurfAddress = "tSmurfAddress";
        final QName dateBuiltInType = new QName(QName.NULL_NS_URI, BuiltInType.DATE.getName());
        final QName stringBuiltInType = new QName(QName.NULL_NS_URI, STRING.getName());

        final ItemDefinition tSmurfAddressCustomDataType = new ItemDefinition();
        tSmurfAddressCustomDataType.setName(new Name(tSmurfAddress));
        tSmurfAddressCustomDataType.getItemComponent().add(new ItemDefinition() {{
            setName(new Name("line1"));
            setTypeRef(stringBuiltInType);
        }});
        tSmurfAddressCustomDataType.getItemComponent().add(new ItemDefinition() {{
            setName(new Name("line2"));
            setTypeRef(stringBuiltInType);
        }});

        final ItemDefinition tSmurfCustomDataType = new ItemDefinition();
        tSmurfCustomDataType.setName(new Name(tSmurf));
        tSmurfCustomDataType.getItemComponent().add(new ItemDefinition() {{
            setName(new Name("dob"));
            setTypeRef(dateBuiltInType);
        }});
        tSmurfCustomDataType.getItemComponent().add(new ItemDefinition() {{
            setName(new Name("address"));
            getItemComponent().add(tSmurfAddressCustomDataType);
        }});

        definitions.getItemDefinition().add(tSmurfCustomDataType);

        final QName inputData1TypeRef = new QName(QName.NULL_NS_URI, tSmurf);
        inputData1.getVariable().setTypeRef(inputData1TypeRef);

        final Optional<DecisionTable> oModel = definition.getModelClass();
        definition.enrich(Optional.of(NODE_UUID), decision, oModel);

        final DecisionTable model = oModel.get();
        assertBasicEnrichment(model);

        final List<InputClause> input = model.getInput();
        assertThat(input.size()).isEqualTo(4);
        assertThat(input.get(0).getInputExpression()).isInstanceOf(InputClauseLiteralExpression.class);
        assertThat(input.get(0).getInputExpression().getText().getValue()).isEqualTo(INPUT_DATA_NAME_2);
        assertThat(input.get(0).getInputExpression().getTypeRef()).isEqualTo(INPUT_DATA_QNAME_2);
        assertThat(input.get(1).getInputExpression()).isInstanceOf(InputClauseLiteralExpression.class);
        assertThat(input.get(1).getInputExpression().getText().getValue()).isEqualTo(INPUT_DATA_NAME_1 + ".address." + tSmurfAddress + ".line1");
        assertThat(input.get(1).getInputExpression().getTypeRef()).isEqualTo(stringBuiltInType);
        assertThat(input.get(2).getInputExpression()).isInstanceOf(InputClauseLiteralExpression.class);
        assertThat(input.get(2).getInputExpression().getText().getValue()).isEqualTo(INPUT_DATA_NAME_1 + ".address." + tSmurfAddress + ".line2");
        assertThat(input.get(2).getInputExpression().getTypeRef()).isEqualTo(stringBuiltInType);
        assertThat(input.get(3).getInputExpression()).isInstanceOf(InputClauseLiteralExpression.class);
        assertThat(input.get(3).getInputExpression().getText().getValue()).isEqualTo(INPUT_DATA_NAME_1 + ".dob");
        assertThat(input.get(3).getInputExpression().getTypeRef()).isEqualTo(dateBuiltInType);

        assertStandardOutputClauseEnrichment(model);
        assertStandardDecisionRuleEnrichment(model, 4, 1);
        assertParentHierarchyEnrichment(model, 4, 1);
    }

    @SuppressWarnings("unchecked")
    private void setupGraph() {
        final Node<Definition, Edge> diagramNode = new NodeImpl<>(UUID.uuid());
        final Node<Definition, Edge> sourceNode1 = new NodeImpl<>(UUID.uuid());
        final Node<Definition, Edge> sourceNode2 = new NodeImpl<>(UUID.uuid());
        final Node<Definition, Edge> targetNode = new NodeImpl<>(NODE_UUID);
        inputData1.getName().setValue(INPUT_DATA_NAME_1);
        inputData2.getName().setValue(INPUT_DATA_NAME_2);
        final QName inputData1QName = new QName(QName.NULL_NS_URI, STRING.getName());
        final QName inputData2QName = new QName(QName.NULL_NS_URI, NUMBER.getName());
        inputData1.getVariable().setTypeRef(inputData1QName);
        inputData2.getVariable().setTypeRef(inputData2QName);

        final Definition<InputData> sourceNode1Definition = new DefinitionImpl<>(inputData1);
        final Definition<InputData> sourceNode2Definition = new DefinitionImpl<>(inputData2);
        sourceNode1.setContent(sourceNode1Definition);
        sourceNode2.setContent(sourceNode2Definition);

        final Edge edge1 = new EdgeImpl<>(UUID.uuid());
        final Edge edge2 = new EdgeImpl<>(UUID.uuid());
        edge1.setTargetNode(targetNode);
        edge1.setSourceNode(sourceNode1);
        edge2.setTargetNode(targetNode);
        edge2.setSourceNode(sourceNode2);

        targetNode.getInEdges().add(edge1);
        targetNode.getInEdges().add(edge2);
        sourceNode1.getOutEdges().add(edge1);
        sourceNode2.getOutEdges().add(edge2);

        final Definition<DMNDiagram> diagramDefinition = new DefinitionImpl<>(diagram);
        diagramNode.setContent(diagramDefinition);

        graph.addNode(diagramNode);
        graph.addNode(targetNode);
        graph.addNode(sourceNode1);
        graph.addNode(sourceNode2);
    }

    @Test
    public void testModelEnrichmentWhenTopLevelDecisionTableWithoutInputData() {
        final String uuid = UUID.uuid();
        final Node<Definition, Edge> node = new NodeImpl<>(uuid);
        graph.addNode(node);

        final Optional<DecisionTable> oModel = definition.getModelClass();
        definition.enrich(Optional.of(uuid), decision, oModel);

        final DecisionTable model = oModel.get();
        assertBasicEnrichment(model);
        assertStandardInputClauseEnrichment(model);
        assertStandardOutputClauseEnrichment(model);
        assertStandardDecisionRuleEnrichment(model, 1, 1);
        assertParentHierarchyEnrichment(model, 1, 1);
    }

    @Test
    public void testModelEnrichmentWhenParentIsContextEntry() {
        final String name = "context-entry";
        final Context context = new Context();
        final ContextEntry contextEntry = new ContextEntry();
        context.getContextEntry().add(contextEntry);
        contextEntry.setVariable(new InformationItem(new Id(), new Description(), new Name(name), OUTPUT_DATA_QNAME));

        final Optional<DecisionTable> oModel = definition.getModelClass();
        oModel.get().setParent(contextEntry);
        contextEntry.setParent(context);
        context.setParent(decision);

        definition.enrich(Optional.empty(), decision, oModel);

        final DecisionTable model = oModel.get();
        assertBasicEnrichment(model);
        assertStandardInputClauseEnrichment(model);

        final List<OutputClause> output = model.getOutput();
        assertThat(output.size()).isEqualTo(1);
        assertThat(output.get(0).getName()).isEqualTo(name);
        assertThat(output.get(0).getTypeRef()).isEqualTo(OUTPUT_DATA_QNAME);

        assertStandardDecisionRuleEnrichment(model, 1, 1);
        assertParentHierarchyEnrichment(model, 1, 1);
    }

    @Test
    public void testModelEnrichmentWhenParentIsContextEntryDefaultResult() {
        final String name = "decision";

        final Context context = new Context();
        final ContextEntry contextEntry = new ContextEntry();
        context.getContextEntry().add(contextEntry);
        contextEntry.setParent(context);
        context.setParent(decision);

        decision.setName(new Name(name));
        decision.getVariable().setTypeRef(OUTPUT_DATA_QNAME);

        final Optional<DecisionTable> oModel = definition.getModelClass();
        oModel.get().setParent(contextEntry);

        definition.enrich(Optional.empty(), decision, oModel);

        final DecisionTable model = oModel.get();
        assertBasicEnrichment(model);
        assertStandardInputClauseEnrichment(model);

        final List<OutputClause> output = model.getOutput();
        assertThat(output.size()).isEqualTo(1);
        assertThat(output.get(0).getName()).isEqualTo(name);
        assertThat(output.get(0).getTypeRef()).isEqualTo(OUTPUT_DATA_QNAME);

        assertStandardDecisionRuleEnrichment(model, 1, 1);
        assertParentHierarchyEnrichment(model, 1, 1);
    }

    @Test
    public void testModelEnrichmentWhenParentIsNestedContextEntryDefaultResult() {
        final String name = "context-entry";

        final Context innerContext = new Context();
        final ContextEntry innerContextEntry = new ContextEntry();
        innerContext.getContextEntry().add(innerContextEntry);
        innerContextEntry.setParent(innerContext);

        final Context outerContext = new Context();
        final ContextEntry outerContextEntry = new ContextEntry();
        outerContext.getContextEntry().add(outerContextEntry);
        outerContextEntry.setParent(outerContext);
        innerContext.setParent(outerContextEntry);
        outerContext.setParent(decision);

        outerContextEntry.setVariable(new InformationItem(new Id(), new Description(), new Name(name), OUTPUT_DATA_QNAME));

        final Optional<DecisionTable> oModel = definition.getModelClass();
        oModel.get().setParent(innerContextEntry);

        definition.enrich(Optional.empty(), decision, oModel);

        final DecisionTable model = oModel.get();
        assertBasicEnrichment(model);
        assertStandardInputClauseEnrichment(model);

        final List<OutputClause> output = model.getOutput();
        assertThat(output.size()).isEqualTo(1);
        assertThat(output.get(0).getName()).isEqualTo(name);
        assertThat(output.get(0).getTypeRef()).isEqualTo(OUTPUT_DATA_QNAME);

        assertStandardDecisionRuleEnrichment(model, 1, 1);
        assertParentHierarchyEnrichment(model, 1, 1);
    }

    @Test
    public void testModelEnrichmentWhenHasExpressionIsHasVariable() {
        decision.setVariable(new InformationItemPrimary(new Id(), new Name(), OUTPUT_DATA_QNAME));

        final Optional<DecisionTable> oModel = definition.getModelClass();

        definition.enrich(Optional.empty(), decision, oModel);

        final DecisionTable model = oModel.get();
        assertBasicEnrichment(model);
        assertStandardInputClauseEnrichment(model);

        final List<OutputClause> output = model.getOutput();
        assertThat(output.size()).isEqualTo(1);
        assertThat(output.get(0).getName()).isEqualTo("output-1");
        assertThat(output.get(0).getTypeRef()).isEqualTo(OUTPUT_DATA_QNAME);

        assertStandardDecisionRuleEnrichment(model, 1, 1);
        assertParentHierarchyEnrichment(model, 1, 1);
    }

    @Test
    public void testModelEnrichmentWhenHasExpressionIsNotHasVariable() {
        final Optional<DecisionTable> oModel = definition.getModelClass();

        definition.enrich(Optional.empty(), decision, oModel);

        final DecisionTable model = oModel.get();
        assertBasicEnrichment(model);
        assertStandardInputClauseEnrichment(model);

        final List<OutputClause> output = model.getOutput();
        assertThat(output.size()).isEqualTo(1);
        assertThat(output.get(0).getName()).isEqualTo("output-1");
        assertThat(output.get(0).getTypeRef()).isEqualTo(BuiltInType.UNDEFINED.asQName());

        assertStandardDecisionRuleEnrichment(model, 1, 1);
        assertParentHierarchyEnrichment(model, 1, 1);
    }

    @Test
    public void testAddInputClauseRequirement() {

        final ItemDefinition tPerson = mock(ItemDefinition.class);
        final ItemDefinition name = mock(ItemDefinition.class);
        final ItemDefinition age = mock(ItemDefinition.class);
        final List<DecisionTableEditorDefinitionEnricher.InputClauseRequirement> inputClauseRequirements = new ArrayList<>();
        final String inputData = "InputData";
        final DecisionTableEditorDefinitionEnricher enricher = new DecisionTableEditorDefinitionEnricher(null, null, itemDefinitionUtils);

        /* === ItemDefinition ===
         * - tPerson (Structure)
         *   - name  (String)
         *   - age   (Number)
         * ======================
         */

        when(name.getName()).thenReturn(new Name("name"));
        when(name.getTypeRef()).thenReturn(STRING.asQName());
        when(name.getItemComponent()).thenReturn(emptyList());

        when(age.getName()).thenReturn(new Name("age"));
        when(age.getTypeRef()).thenReturn(NUMBER.asQName());
        when(age.getItemComponent()).thenReturn(emptyList());

        when(tPerson.getName()).thenReturn(new Name("tPerson"));
        when(tPerson.getTypeRef()).thenReturn(null);
        when(tPerson.getItemComponent()).thenReturn(asList(name, age));

        enricher.addInputClauseRequirement(tPerson, inputClauseRequirements, inputData);

        assertEquals(2, inputClauseRequirements.size());

        final DecisionTableEditorDefinitionEnricher.InputClauseRequirement inputClause1 = inputClauseRequirements.get(0);
        final DecisionTableEditorDefinitionEnricher.InputClauseRequirement inputClause2 = inputClauseRequirements.get(1);

        assertEquals("InputData.name", inputClause1.text);
        assertEquals(STRING.getName(), inputClause1.typeRef.getLocalPart());

        assertEquals("InputData.age", inputClause2.text);
        assertEquals(NUMBER.getName(), inputClause2.typeRef.getLocalPart());
    }

    @Test
    public void testAddInputClauseRequirementWhenDataTypeIsStructureAndDontHaveFields() {

        final ItemDefinition tPerson = mock(ItemDefinition.class);
        final String inputData = "InputData";
        final List<DecisionTableEditorDefinitionEnricher.InputClauseRequirement> inputClauseRequirements = new ArrayList<>();
        final ItemDefinitionUtils itemDefinitionUtils = new ItemDefinitionUtils(mock(DMNGraphUtils.class));
        final DecisionTableEditorDefinitionEnricher enricher = new DecisionTableEditorDefinitionEnricher(null, null, itemDefinitionUtils);

        when(tPerson.getName()).thenReturn(new Name("tPerson"));
        when(tPerson.getTypeRef()).thenReturn(null);
        when(tPerson.getItemComponent()).thenReturn(emptyList());

        enricher.addInputClauseRequirement(tPerson, inputClauseRequirements, inputData);

        assertEquals(1, inputClauseRequirements.size());

        assertEquals("InputData", inputClauseRequirements.get(0).text);
        assertEquals("tPerson", inputClauseRequirements.get(0).typeRef.getLocalPart());
    }
}
