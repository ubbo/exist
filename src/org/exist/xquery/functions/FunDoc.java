/*
 *  eXist Open Source Native XML Database
 *  Copyright (C) 2001-03 Wolfgang M. Meier
 *  wolfgang@exist-db.org
 *  http://exist.sourceforge.net
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
package org.exist.xquery.functions;

import org.exist.dom.DocumentImpl;
import org.exist.dom.NodeProxy;
import org.exist.dom.QName;
import org.exist.security.Permission;
import org.exist.security.PermissionDeniedException;
import org.exist.util.Lock;
import org.exist.util.LockException;
import org.exist.xquery.Cardinality;
import org.exist.xquery.Dependency;
import org.exist.xquery.Function;
import org.exist.xquery.FunctionSignature;
import org.exist.xquery.Module;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.value.Item;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.SequenceType;
import org.exist.xquery.value.Type;
import org.w3c.dom.Node;

/**
 * Implements the built-in fn:doc() function.
 * 
 * This will be replaced by XQuery's fn:doc() function.
 * 
 * @author wolf
 */
public class FunDoc extends Function {

	public final static FunctionSignature signature =
		new FunctionSignature(
			new QName("doc", Module.BUILTIN_FUNCTION_NS),
			"Includes one or more documents "
				+ "into the input sequence. Currently, "
				+ "eXist interprets each argument as a path pointing to a "
				+ "document in the database, as for example, '/db/shakespeare/plays/hamlet.xml'. "
				+ "If the path is relative, it is resolved relative to the base URI property from the static context.",
			new SequenceType[] { new SequenceType(Type.STRING, Cardinality.ZERO_OR_ONE)},
			new SequenceType(Type.NODE, Cardinality.ZERO_OR_ONE));

	private NodeProxy cachedNode = null;
	private String cachedPath = null;
	
	/**
	 * @param context
	 * @param signature
	 */
	public FunDoc(XQueryContext context) {
		super(context, signature);
	}

	/* (non-Javadoc)
	 * @see org.exist.xquery.Function#getDependencies()
	 */
	public int getDependencies() {
		return Dependency.CONTEXT_SET;
	}

	/* (non-Javadoc)
	 * @see org.exist.xquery.Expression#eval(org.exist.dom.DocumentSet, org.exist.xquery.value.Sequence, org.exist.xquery.value.Item)
	 */
	public Sequence eval(Sequence contextSequence, Item contextItem)
		throws XPathException {
		Sequence arg =
			getArgument(0).eval(contextSequence, contextItem);
		if(arg.getLength() == 0)
			return Sequence.EMPTY_SEQUENCE;
		String path = arg.itemAt(0).getStringValue();
		if (path.length() == 0)
			throw new XPathException(getASTNode(), "Invalid argument to fn:doc function: empty string is not allowed here.");
        
        // relative URL: add the current base URI
		if (path.charAt(0) != '/')
            path = context.getBaseURI() + '/' + path;
        
		// check if the loaded documents should remain locked
        boolean lockOnLoad = context.lockDocumentsOnLoad();
        Lock dlock = null;
        
		// if the expression occurs in a nested context, we might have cached the
        // document set
		if(path.equals(cachedPath) && cachedNode != null) {
		    dlock = cachedNode.getDocument().getUpdateLock();
		    try {
		        // wait for pending updates by acquiring a lock
		        dlock.acquire(Lock.READ_LOCK);
		        return cachedNode;
		    } catch (LockException e) {
		        throw new XPathException(getASTNode(), "Failed to acquire lock on document " + path);
            } finally {
                dlock.release(Lock.READ_LOCK);
		    }
		}
        
        DocumentImpl doc = null;
		try {
            // try to open the document and acquire a lock
		    doc = (DocumentImpl) context.getBroker().openDocument(path, Lock.READ_LOCK);
		    if(doc == null) {
//		        LOG.debug("Document " + path + " not found!");
                return Sequence.EMPTY_SEQUENCE;
            }
		    if(!doc.getPermissions().validate(context.getUser(), Permission.READ)) {
                doc.getUpdateLock().release(Lock.READ_LOCK);
                doc = null;
                throw new XPathException(getASTNode(), "Insufficient privileges to read resource " + path);
            }
			cachedPath = path;
			cachedNode = new NodeProxy(doc, -1, Node.DOCUMENT_NODE);
            if(lockOnLoad) {
                // add the document to the list of locked documents
                context.getLockedDocuments().add(doc);
            }
			return cachedNode;
		} catch (PermissionDeniedException e) {
			throw new XPathException(getASTNode(),
				"Permission denied: unable to load document " + path);
        } finally {
            // release all locks unless lockOnLoad is true
		    if(!lockOnLoad && doc != null)
		        doc.getUpdateLock().release(Lock.READ_LOCK);
		}
	}

	/* (non-Javadoc)
	 * @see org.exist.xquery.PathExpr#resetState()
	 */
	public void resetState() {
		cachedNode = null;
		cachedPath = null;
		getArgument(0).resetState();
	}
}
