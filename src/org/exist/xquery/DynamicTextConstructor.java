/*
 *  eXist Open Source Native XML Database
 *  Copyright (C) 2001-04 Wolfgang M. Meier
 *  wolfgang@exist-db.org
 *  http://exist-db.org
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 * 
 *  $Id$
 */
package org.exist.xquery;

import org.exist.memtree.DocumentImpl;
import org.exist.memtree.MemTreeBuilder;
import org.exist.xquery.util.ExpressionDumper;
import org.exist.xquery.value.Item;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.SequenceIterator;


/**
 * Implements a dynamic text constructor. Contrary to {@link org.exist.xquery.TextConstructor},
 * the character content of a DynamicTextConstructor is determined only at evaluation time.
 * 
 * @author wolf
 */
public class DynamicTextConstructor extends NodeConstructor {

    private Expression content;
    
    /**
     * @param context
     */
    public DynamicTextConstructor(XQueryContext context, Expression contentExpr) {
        super(context);
        this.content = new Atomize(context, contentExpr);
    }

    /* (non-Javadoc)
     * @see org.exist.xquery.Expression#analyze(org.exist.xquery.Expression)
     */
    public void analyze(Expression parent, int flags) throws XPathException {
        content.analyze(this, flags);
    }
    
    /* (non-Javadoc)
     * @see org.exist.xquery.Expression#eval(org.exist.xquery.value.Sequence, org.exist.xquery.value.Item)
     */
    public Sequence eval(Sequence contextSequence, Item contextItem) throws XPathException {
        if (context.getProfiler().isEnabled()) {
            context.getProfiler().start(this);       
            context.getProfiler().message(this, Profiler.DEPENDENCIES, "DEPENDENCIES", Dependency.getDependenciesName(this.getDependencies()));
            if (contextSequence != null)
                context.getProfiler().message(this, Profiler.START_SEQUENCES, "CONTEXT SEQUENCE", contextSequence);
            if (contextItem != null)
                context.getProfiler().message(this, Profiler.START_SEQUENCES, "CONTEXT ITEM", contextItem.toSequence());
        }
        
        Sequence contentSeq = content.eval(contextSequence, contextItem);
        
        if(contentSeq.getLength() == 0)
            return Sequence.EMPTY_SEQUENCE;
        
        MemTreeBuilder builder = context.getDocumentBuilder();
		context.proceed(this, builder);
		StringBuffer buf = new StringBuffer();
        for(SequenceIterator i = contentSeq.iterate(); i.hasNext(); ) {
            context.proceed(this, builder);
            Item next = i.nextItem();
            if(buf.length() > 0)
                buf.append(' ');
            buf.append(next.toString());
        }
        int nodeNr = builder.characters(buf);
        return ((DocumentImpl)builder.getDocument()).getNode(nodeNr);
    }
    
    /* (non-Javadoc)
     * @see org.exist.xquery.Expression#dump(org.exist.xquery.util.ExpressionDumper)
     */
    public void dump(ExpressionDumper dumper) {
        dumper.display("text {");
        dumper.startIndent();
        content.dump(dumper);
        dumper.endIndent();
        dumper.nl().display("}");
    }
    
    public String toString() {
    	StringBuffer result = new StringBuffer();
    	result.append("text {");        
    	result.append(content.toString());        
    	result.append("}");
    	return result.toString();
    }    

}
