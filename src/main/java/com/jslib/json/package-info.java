/**
 * JSON parser and serializer for objects with not restricted complexity. JSON package uses {@link com.jslib.json.JsonImpl} 
 * facade to supplies (de)serialization services.
 * All implementation is private so that in order to use JSON package one needs to know only about facade. As a
 * consequence using JSON package is straightforward: invoke facade supplied methods, see samples.
 * <pre>
 *	// parse Page instance from JSON object stream
 *	Page page = JSON.parse(reader, Page.class);
 *	
 *	// parse not homogeneous array from JSON array stream
 *	Object[] parameters = JSON.parse(reader, new Type[] { double.class, Page.class, String.class, boolean.class });
 *  
 *	// parse list of objects from JSON formatted string  
 *	List&lt;Page&gt; pages = JSON.parse(json, new GType(List.class, Page.class));
 *
 *	// serialize Page instance on JSON stream  
 *	JSON.stringify(writer, page);
 *
 *	// serialize Page instance to JSON formatted string
 *	String json = JSON.stringify(page);
 * </pre>
 * Method names used on JSON facade should be familiar to ECMA Script programmers.
 * 
 * <h5>Parameterized Types</h5>
 * Parameterized types are supported and processed reflectively. This is based on Java API stating in couple 
 * classes reference that parameterized types are present into bytecode. For example see this excerpt from 
 * Method.getGenericParameterTypes API: <em>If a formal parameter type is a parameterized type, the Type object 
 * returned for it must accurately reflect the actual type parameters used in the source code.</em>
 * <p>
 * Anyway, do not confuse with type variables, a.k.a. type parameters. Those are not present into bytecode and are
 * not considered by JSON package logic.
 * 
 * <h5>Best Effort</h5>
 * JSON parser from this package is relaxed and follows a best effort approach. If a named property from JSON stream 
 * does not have the same name field into target object, parser just warn on log and ignore. Also, fields from target 
 * object with no values into JSON stream are set to null. On serialization all fields are processed less static and transient.
 * 
 * <h5>Inband Type Information</h5>
 * JSON parser and serializer support an extension to JSON protocol that allows for inband type information processing.
 * It is named inband because type information is serialized together with value object, in the same stream. A JSON stream 
 * with inbad type information may look like below. Please note <code>class</code> property from JSON start.
 * <pre>
 *  {"class":"js.net.Event","id":1234 ... }
 * </pre>
 * Implementation note: current implementation does not support inband type information for JSON arrays; only objects. Also, for
 * nested objects only outer most one have <code>class</code> field. Inner objects type is inferred reflectively.
 * 
 * @author Iulian Rotaru
 */
package com.jslib.json;

