/*
 * Copyright (C) 2014 mkleint
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.tropyx.nb_puppet.semantic;

import com.tropyx.nb_puppet.PPConstants;
import com.tropyx.nb_puppet.parser.PElement;
import com.tropyx.nb_puppet.parser.PFunction;
import com.tropyx.nb_puppet.parser.PResource;
import com.tropyx.nb_puppet.parser.PResourceAttribute;
import com.tropyx.nb_puppet.parser.PVariable;
import com.tropyx.nb_puppet.parser.PVariableDefinition;
import com.tropyx.nb_puppet.parser.PuppetParserResult;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.SwingUtilities;
import javax.swing.text.AttributeSet;
import javax.swing.text.Document;
import org.netbeans.api.editor.mimelookup.MimeLookup;
import org.netbeans.api.editor.mimelookup.MimePath;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.editor.settings.FontColorSettings;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.parsing.spi.ParserResultTask;
import org.netbeans.modules.parsing.spi.Scheduler;
import org.netbeans.modules.parsing.spi.SchedulerEvent;
import org.netbeans.modules.parsing.spi.SchedulerTask;
import org.netbeans.modules.parsing.spi.TaskFactory;
import org.netbeans.spi.editor.highlighting.support.OffsetsBag;

public class SemanticColoring extends ParserResultTask<PuppetParserResult> {
    public static final String COLOR_RESOURCEPARAMETER = "resource-parameter";
    public static final String COLOR_RESOURCENAME = "resource-name";
    public static final String COLOR_METHODDECLARATION = "method-declaration";
    public static final String COLOR_LOCAL_VARIABLE = "local-variable";

    private final static List<String> metaparameters = Arrays.asList(new String[] {
       "alias", "audit", "before", "loglevel", "noop", "notify",
       "require", "schedule", "stage", "subscribe", "tag"
    });

    public SemanticColoring() {
        super();
    }

    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    public Class<? extends Scheduler> getSchedulerClass() {
        return Scheduler.EDITOR_SENSITIVE_TASK_SCHEDULER;
    }

    @Override
    public void cancel() {

    }

    @Override
    public void run(PuppetParserResult result, SchedulerEvent event) {
        final Document doc = result.getSnapshot().getSource().getDocument(false);
        if (doc == null) {
            return;
        }
        final PElement root = result.getRootNode();
        final OffsetsBag rootBag = getSemanticHighlightsBag(doc);
        final FontColorSettings fcs = MimeLookup.getLookup(MimePath.get(PPConstants.MIME_TYPE)).lookup(FontColorSettings.class);
        if (root != null) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    OffsetsBag bag = new OffsetsBag(doc);
                    AttributeSet functionAttrs = fcs.getTokenFontColors(COLOR_METHODDECLARATION);
                    for (PFunction function : root.getChildrenOfType(PFunction.class, true)) {
                        bag.addHighlight(function.getOffset(), function.getOffset() + function.getName().length(), functionAttrs);
                    }
                    AttributeSet resAttrs = fcs.getTokenFontColors(COLOR_RESOURCENAME);
                    for (PResource res : root.getChildrenOfType(PResource.class, true)) {
                        bag.addHighlight(res.getOffset(), res.getOffset() + res.getResourceType().length(), resAttrs);
                    }
                    AttributeSet resAttrAttrs = fcs.getTokenFontColors(COLOR_RESOURCEPARAMETER);
                    AttributeSet metaresAttrAttrs = fcs.getTokenFontColors("resource-metaparameter");
                    for (PResourceAttribute attr : root.getChildrenOfType(PResourceAttribute.class, true)) {
                        bag.addHighlight(attr.getOffset(), attr.getOffset() + attr.getName().length(), metaparameters.contains(attr.getName()) ? metaresAttrAttrs : resAttrAttrs);
                    }
                    Set<String> varNames = new HashSet<>();
                    AttributeSet localVarsAttrs = fcs.getTokenFontColors(COLOR_LOCAL_VARIABLE);
                    for (PVariableDefinition v : root.getChildrenOfType(PVariableDefinition.class, true)) {
                        varNames.add(v.getName());
                        bag.addHighlight(v.getOffset(), v.getOffset() + v.getName().length(), localVarsAttrs);
                    }
                    for (PVariable v : root.getChildrenOfType(PVariable.class, true)) {
                        if (varNames.contains(v.getName())) {
                            bag.addHighlight(v.getOffset(), v.getOffset() + v.getName().length(), localVarsAttrs);
                        }
                    }

                    rootBag.setHighlights(bag);
                }
            });

        }
    }

    private static final Object SEMANTIC_HIGHLIGHTS = new Object();

    public static OffsetsBag getSemanticHighlightsBag(Document doc) {
        OffsetsBag bag = (OffsetsBag) doc.getProperty(SEMANTIC_HIGHLIGHTS);

        if (bag == null) {
            doc.putProperty(SEMANTIC_HIGHLIGHTS, bag = new OffsetsBag(doc));
        }

        return bag;
    }

    @MimeRegistration(mimeType = PPConstants.MIME_TYPE, service = TaskFactory.class)
    public static class Factory extends TaskFactory {

        @Override
        public Collection<? extends SchedulerTask> create(Snapshot snapshot) {
            return Collections.singleton(new SemanticColoring());
        }
    }
}
