/**
 * MVEL 2.0
 * Copyright (C) 2007 The Codehaus
 * Mike Brock, Dhanji Prasanna, John Graham, Mark Proctor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.simpleframework.lib.org.mvel2.optimizers.impl.asm;

import static java.lang.String.valueOf;
import static java.lang.System.getProperty;
import static java.lang.Thread.currentThread;
import static java.lang.reflect.Array.getLength;
import static java.lang.reflect.Modifier.FINAL;
import static java.lang.reflect.Modifier.STATIC;
import static net.simpleframework.lib.org.mvel2.DataConversion.canConvert;
import static net.simpleframework.lib.org.mvel2.DataConversion.convert;
import static net.simpleframework.lib.org.mvel2.MVEL.eval;
import static net.simpleframework.lib.org.mvel2.MVEL.isAdvancedDebugging;
import static net.simpleframework.lib.org.mvel2.ast.TypeDescriptor.getClassReference;
import static net.simpleframework.lib.org.mvel2.integration.GlobalListenerFactory.hasGetListeners;
import static net.simpleframework.lib.org.mvel2.integration.GlobalListenerFactory.notifyGetListeners;
import static net.simpleframework.lib.org.mvel2.integration.PropertyHandlerFactory.getNullMethodHandler;
import static net.simpleframework.lib.org.mvel2.integration.PropertyHandlerFactory.getNullPropertyHandler;
import static net.simpleframework.lib.org.mvel2.integration.PropertyHandlerFactory.getPropertyHandler;
import static net.simpleframework.lib.org.mvel2.integration.PropertyHandlerFactory.hasNullMethodHandler;
import static net.simpleframework.lib.org.mvel2.integration.PropertyHandlerFactory.hasNullPropertyHandler;
import static net.simpleframework.lib.org.mvel2.integration.PropertyHandlerFactory.hasPropertyHandler;
import static net.simpleframework.lib.org.mvel2.util.ArrayTools.findFirst;
import static net.simpleframework.lib.org.mvel2.util.ParseTools.EMPTY_OBJ_ARR;
import static net.simpleframework.lib.org.mvel2.util.ParseTools.balancedCapture;
import static net.simpleframework.lib.org.mvel2.util.ParseTools.balancedCaptureWithLineAccounting;
import static net.simpleframework.lib.org.mvel2.util.ParseTools.captureContructorAndResidual;
import static net.simpleframework.lib.org.mvel2.util.ParseTools.determineActualTargetMethod;
import static net.simpleframework.lib.org.mvel2.util.ParseTools.findClass;
import static net.simpleframework.lib.org.mvel2.util.ParseTools.getBaseComponentType;
import static net.simpleframework.lib.org.mvel2.util.ParseTools.getBestCandidate;
import static net.simpleframework.lib.org.mvel2.util.ParseTools.getBestConstructorCandidate;
import static net.simpleframework.lib.org.mvel2.util.ParseTools.getSubComponentType;
import static net.simpleframework.lib.org.mvel2.util.ParseTools.getWidenedTarget;
import static net.simpleframework.lib.org.mvel2.util.ParseTools.isPrimitiveWrapper;
import static net.simpleframework.lib.org.mvel2.util.ParseTools.parseMethodOrConstructor;
import static net.simpleframework.lib.org.mvel2.util.ParseTools.parseParameterList;
import static net.simpleframework.lib.org.mvel2.util.ParseTools.repeatChar;
import static net.simpleframework.lib.org.mvel2.util.ParseTools.subCompileExpression;
import static net.simpleframework.lib.org.mvel2.util.ParseTools.subset;
import static net.simpleframework.lib.org.mvel2.util.PropertyTools.getFieldOrAccessor;
import static net.simpleframework.lib.org.mvel2.util.PropertyTools.getFieldOrWriteAccessor;
import static net.simpleframework.lib.org.mvel2.util.ReflectionUtil.toNonPrimitiveArray;
import static net.simpleframework.lib.org.mvel2.util.ReflectionUtil.toNonPrimitiveType;
import static net.simpleframework.lib.org.mvel2.util.Varargs.normalizeArgsForVarArgs;
import static net.simpleframework.lib.org.mvel2.util.Varargs.paramTypeVarArgsSafe;
import static net.simpleframework.lib.org.objectweb.asm.Opcodes.AALOAD;
import static net.simpleframework.lib.org.objectweb.asm.Opcodes.AASTORE;
import static net.simpleframework.lib.org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static net.simpleframework.lib.org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static net.simpleframework.lib.org.objectweb.asm.Opcodes.ACONST_NULL;
import static net.simpleframework.lib.org.objectweb.asm.Opcodes.ALOAD;
import static net.simpleframework.lib.org.objectweb.asm.Opcodes.ANEWARRAY;
import static net.simpleframework.lib.org.objectweb.asm.Opcodes.ARETURN;
import static net.simpleframework.lib.org.objectweb.asm.Opcodes.ARRAYLENGTH;
import static net.simpleframework.lib.org.objectweb.asm.Opcodes.ASTORE;
import static net.simpleframework.lib.org.objectweb.asm.Opcodes.ATHROW;
import static net.simpleframework.lib.org.objectweb.asm.Opcodes.BALOAD;
import static net.simpleframework.lib.org.objectweb.asm.Opcodes.BASTORE;
import static net.simpleframework.lib.org.objectweb.asm.Opcodes.BIPUSH;
import static net.simpleframework.lib.org.objectweb.asm.Opcodes.CALOAD;
import static net.simpleframework.lib.org.objectweb.asm.Opcodes.CASTORE;
import static net.simpleframework.lib.org.objectweb.asm.Opcodes.CHECKCAST;
import static net.simpleframework.lib.org.objectweb.asm.Opcodes.DALOAD;
import static net.simpleframework.lib.org.objectweb.asm.Opcodes.DASTORE;
import static net.simpleframework.lib.org.objectweb.asm.Opcodes.DUP;
import static net.simpleframework.lib.org.objectweb.asm.Opcodes.DUP_X1;
import static net.simpleframework.lib.org.objectweb.asm.Opcodes.DUP_X2;
import static net.simpleframework.lib.org.objectweb.asm.Opcodes.FALOAD;
import static net.simpleframework.lib.org.objectweb.asm.Opcodes.FASTORE;
import static net.simpleframework.lib.org.objectweb.asm.Opcodes.GETFIELD;
import static net.simpleframework.lib.org.objectweb.asm.Opcodes.GETSTATIC;
import static net.simpleframework.lib.org.objectweb.asm.Opcodes.GOTO;
import static net.simpleframework.lib.org.objectweb.asm.Opcodes.IALOAD;
import static net.simpleframework.lib.org.objectweb.asm.Opcodes.IASTORE;
import static net.simpleframework.lib.org.objectweb.asm.Opcodes.ICONST_0;
import static net.simpleframework.lib.org.objectweb.asm.Opcodes.ICONST_1;
import static net.simpleframework.lib.org.objectweb.asm.Opcodes.ICONST_2;
import static net.simpleframework.lib.org.objectweb.asm.Opcodes.ICONST_3;
import static net.simpleframework.lib.org.objectweb.asm.Opcodes.ICONST_4;
import static net.simpleframework.lib.org.objectweb.asm.Opcodes.ICONST_5;
import static net.simpleframework.lib.org.objectweb.asm.Opcodes.IFEQ;
import static net.simpleframework.lib.org.objectweb.asm.Opcodes.IFNONNULL;
import static net.simpleframework.lib.org.objectweb.asm.Opcodes.IF_ICMPLT;
import static net.simpleframework.lib.org.objectweb.asm.Opcodes.ILOAD;
import static net.simpleframework.lib.org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static net.simpleframework.lib.org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static net.simpleframework.lib.org.objectweb.asm.Opcodes.INVOKESTATIC;
import static net.simpleframework.lib.org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static net.simpleframework.lib.org.objectweb.asm.Opcodes.ISTORE;
import static net.simpleframework.lib.org.objectweb.asm.Opcodes.LALOAD;
import static net.simpleframework.lib.org.objectweb.asm.Opcodes.LASTORE;
import static net.simpleframework.lib.org.objectweb.asm.Opcodes.NEW;
import static net.simpleframework.lib.org.objectweb.asm.Opcodes.POP;
import static net.simpleframework.lib.org.objectweb.asm.Opcodes.PUTFIELD;
import static net.simpleframework.lib.org.objectweb.asm.Opcodes.RETURN;
import static net.simpleframework.lib.org.objectweb.asm.Opcodes.SALOAD;
import static net.simpleframework.lib.org.objectweb.asm.Opcodes.SASTORE;
import static net.simpleframework.lib.org.objectweb.asm.Opcodes.SIPUSH;
import static net.simpleframework.lib.org.objectweb.asm.Opcodes.SWAP;
import static net.simpleframework.lib.org.objectweb.asm.Type.getConstructorDescriptor;
import static net.simpleframework.lib.org.objectweb.asm.Type.getDescriptor;
import static net.simpleframework.lib.org.objectweb.asm.Type.getInternalName;
import static net.simpleframework.lib.org.objectweb.asm.Type.getMethodDescriptor;
import static net.simpleframework.lib.org.objectweb.asm.Type.getType;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import net.simpleframework.lib.org.mvel2.CompileException;
import net.simpleframework.lib.org.mvel2.DataConversion;
import net.simpleframework.lib.org.mvel2.MVEL;
import net.simpleframework.lib.org.mvel2.OptimizationFailure;
import net.simpleframework.lib.org.mvel2.ParserContext;
import net.simpleframework.lib.org.mvel2.PropertyAccessException;
import net.simpleframework.lib.org.mvel2.ast.Function;
import net.simpleframework.lib.org.mvel2.ast.TypeDescriptor;
import net.simpleframework.lib.org.mvel2.ast.WithNode;
import net.simpleframework.lib.org.mvel2.compiler.Accessor;
import net.simpleframework.lib.org.mvel2.compiler.AccessorNode;
import net.simpleframework.lib.org.mvel2.compiler.ExecutableAccessor;
import net.simpleframework.lib.org.mvel2.compiler.ExecutableLiteral;
import net.simpleframework.lib.org.mvel2.compiler.ExecutableStatement;
import net.simpleframework.lib.org.mvel2.compiler.PropertyVerifier;
import net.simpleframework.lib.org.mvel2.integration.GlobalListenerFactory;
import net.simpleframework.lib.org.mvel2.integration.PropertyHandler;
import net.simpleframework.lib.org.mvel2.integration.VariableResolverFactory;
import net.simpleframework.lib.org.mvel2.optimizers.AbstractOptimizer;
import net.simpleframework.lib.org.mvel2.optimizers.AccessorOptimizer;
import net.simpleframework.lib.org.mvel2.optimizers.OptimizationNotSupported;
import net.simpleframework.lib.org.mvel2.optimizers.impl.refl.nodes.Union;
import net.simpleframework.lib.org.mvel2.util.JITClassLoader;
import net.simpleframework.lib.org.mvel2.util.MVELClassLoader;
import net.simpleframework.lib.org.mvel2.util.MethodStub;
import net.simpleframework.lib.org.mvel2.util.ParseTools;
import net.simpleframework.lib.org.mvel2.util.PropertyTools;
import net.simpleframework.lib.org.mvel2.util.StringAppender;
import net.simpleframework.lib.org.objectweb.asm.ClassWriter;
import net.simpleframework.lib.org.objectweb.asm.Label;
import net.simpleframework.lib.org.objectweb.asm.MethodVisitor;
import net.simpleframework.lib.org.objectweb.asm.Opcodes;

/**
 * Implementation of the MVEL Just-in-Time (JIT) compiler for Property Accessors
 * using the ASM bytecode engineering library.
 * <p/>
 */
@SuppressWarnings({ "TypeParameterExplicitlyExtendsObject", "unchecked", "UnusedDeclaration" })
public class ASMAccessorOptimizer extends AbstractOptimizer implements AccessorOptimizer {
	private static final String MAP_IMPL = "java/util/HashMap";

	private static String LIST_IMPL;
	private static String NAMESPACE;
	private static final int OPCODES_VERSION;

	static {
		final String javaVersion = getProperty("java.version");
		if (javaVersion.startsWith("1.4")) {
			OPCODES_VERSION = Opcodes.V1_4;
		} else if (javaVersion.startsWith("1.5")) {
			OPCODES_VERSION = Opcodes.V1_5;
		} else if (javaVersion.startsWith("1.6") || javaVersion.startsWith("1.7")) {
			OPCODES_VERSION = Opcodes.V1_6;
		} else {
			OPCODES_VERSION = Opcodes.V1_2;
		}

		final String defaultNameSapce = getProperty("mvel2.namespace");
		if (defaultNameSapce == null) {
			NAMESPACE = "org/mvel2/";
		} else {
			NAMESPACE = defaultNameSapce;
		}

		final String jitListImpl = getProperty("mvel2.jit.list_impl");
		if (jitListImpl == null) {
			LIST_IMPL = NAMESPACE + "util/FastList";
		} else {
			LIST_IMPL = jitListImpl;
		}
	}

	private Object ctx;
	private Object thisRef;

	private VariableResolverFactory variableFactory;

	private static final Object[] EMPTYARG = new Object[0];
	private static final Class[] EMPTYCLS = new Class[0];

	private boolean first = true;
	private boolean noinit = false;
	private boolean deferFinish = false;
	private boolean literal = false;

	private boolean propNull = false;
	private boolean methNull = false;

	private String className;
	private ClassWriter cw;
	private MethodVisitor mv;

	private Object val;
	private int stacksize = 1;
	private final int maxlocals = 1;
	private long time;

	private ArrayList<ExecutableStatement> compiledInputs;

	private Class ingressType;
	private Class returnType;

	private int compileDepth = 0;

	@SuppressWarnings({ "StringBufferField" })
	private StringAppender buildLog;

	public ASMAccessorOptimizer() {
		// do this to confirm we're running the correct version
		// otherwise should create a verification error in VM
		new ClassWriter(ClassWriter.COMPUTE_MAXS);
	}

	private ASMAccessorOptimizer(final ClassWriter cw, final MethodVisitor mv,
			final ArrayList<ExecutableStatement> compiledInputs, final String className,
			final StringAppender buildLog, final int compileDepth) {
		this.cw = cw;
		this.mv = mv;
		this.compiledInputs = compiledInputs;
		this.className = className;
		this.buildLog = buildLog;
		this.compileDepth = compileDepth + 1;

		noinit = true;
		deferFinish = true;
	}

	/**
	 * Does all the boilerplate for initiating the JIT.
	 */
	private void _initJIT() {
		if (isAdvancedDebugging()) {
			buildLog = new StringAppender();
		}

		cw = new ClassWriter(ClassWriter.COMPUTE_MAXS + ClassWriter.COMPUTE_FRAMES);

		synchronized (Runtime.getRuntime()) {
			cw.visit(OPCODES_VERSION, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER,
					className = "ASMAccessorImpl_" + valueOf(cw.hashCode()).replaceAll("\\-", "_")
							+ (System.currentTimeMillis() / 10) + ((int) Math.random() * 100), null,
					"java/lang/Object", new String[] { NAMESPACE + "compiler/Accessor" });
		}

		final MethodVisitor m = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);

		m.visitCode();
		m.visitVarInsn(Opcodes.ALOAD, 0);
		m.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
		m.visitInsn(RETURN);

		m.visitMaxs(1, 1);
		m.visitEnd();

		(mv = cw.visitMethod(ACC_PUBLIC, "getValue", "(Ljava/lang/Object;Ljava/lang/Object;L"
				+ NAMESPACE + "integration/VariableResolverFactory;)Ljava/lang/Object;", null, null))
				.visitCode();
	}

	private void _initJIT2() {
		if (isAdvancedDebugging()) {
			buildLog = new StringAppender();
		}

		cw = new ClassWriter(ClassWriter.COMPUTE_MAXS + ClassWriter.COMPUTE_FRAMES);

		synchronized (Runtime.getRuntime()) {
			cw.visit(OPCODES_VERSION, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER,
					className = "ASMAccessorImpl_" + valueOf(cw.hashCode()).replaceAll("\\-", "_")
							+ (System.currentTimeMillis() / 10) + ((int) Math.random() * 100), null,
					"java/lang/Object", new String[] { NAMESPACE + "compiler/Accessor" });
		}

		final MethodVisitor m = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);

		m.visitCode();
		m.visitVarInsn(Opcodes.ALOAD, 0);
		m.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
		m.visitInsn(RETURN);

		m.visitMaxs(1, 1);
		m.visitEnd();

		(mv = cw.visitMethod(ACC_PUBLIC, "setValue", "(Ljava/lang/Object;Ljava/lang/Object;L"
				+ NAMESPACE
				+ "integration/VariableResolverFactory;Ljava/lang/Object;)Ljava/lang/Object;", null,
				null)).visitCode();
	}

	@Override
	public Accessor optimizeAccessor(final ParserContext pCtx, final char[] property,
			final int start, final int offset, final Object staticContext, final Object thisRef,
			final VariableResolverFactory factory, final boolean root, final Class ingressType) {
		time = System.currentTimeMillis();

		if (compiledInputs == null) {
			compiledInputs = new ArrayList<ExecutableStatement>();
		}

		this.start = cursor = start;
		this.end = start + offset;
		this.length = end - this.start;

		this.first = true;
		this.val = null;

		this.pCtx = pCtx;
		this.expr = property;
		this.ctx = staticContext;
		this.thisRef = thisRef;
		this.variableFactory = factory;
		this.ingressType = ingressType;

		if (!noinit) {
			_initJIT();
		}
		return compileAccessor();
	}

	@Override
	public Accessor optimizeSetAccessor(final ParserContext pCtx, final char[] property,
			final int start, final int offset, Object ctx, final Object thisRef,
			final VariableResolverFactory factory, final boolean rootThisRef, Object value,
			final Class ingressType) {
		this.expr = property;
		this.start = this.cursor = start;
		this.end = start + offset;
		this.length = start + offset;

		this.first = true;
		this.ingressType = ingressType;

		compiledInputs = new ArrayList<ExecutableStatement>();

		this.pCtx = pCtx;
		this.ctx = ctx;
		this.thisRef = thisRef;
		this.variableFactory = factory;

		char[] root = null;

		final PropertyVerifier verifier = new PropertyVerifier(property, this.pCtx = pCtx);

		final int split = findLastUnion();

		if (split != -1) {
			root = subset(property, 0, split);
		}

		final AccessorNode rootAccessor = null;

		_initJIT2();

		if (root != null) {
			final int _length = this.length;
			final int _end = this.end;
			final char[] _expr = this.expr;

			this.length = end = (this.expr = root).length;

			// run the compiler but don't finish building.
			deferFinish = true;
			noinit = true;

			compileAccessor();
			ctx = this.val;

			this.expr = _expr;
			this.cursor = start + root.length + 1;
			this.length = _length - root.length - 1;
			this.end = this.cursor + this.length;
		} else {
			assert debug("ALOAD 1");
			mv.visitVarInsn(ALOAD, 1);
		}

		try {

			skipWhitespace();

			if (collection) {
				final int st = cursor;
				whiteSpaceSkip();

				if (st == end) {
					throw new PropertyAccessException("unterminated '['", expr, start);
				}

				if (scanTo(']')) {
					throw new PropertyAccessException("unterminated '['", expr, start);
				}

				final String ex = new String(expr, st, cursor - st).trim();

				assert debug("CHECKCAST " + ctx.getClass().getName());
				mv.visitTypeInsn(CHECKCAST, getInternalName(ctx.getClass()));

				if (ctx instanceof Map) {
					if (MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING
							&& hasPropertyHandler(Map.class)) {
						propHandlerByteCodePut(ex, ctx, Map.class, value);
					} else {
						// noinspection unchecked
						((Map) ctx).put(eval(ex, ctx, variableFactory),
								convert(value, returnType = verifier.analyze()));

						writeLiteralOrSubexpression(subCompileExpression(ex.toCharArray(), pCtx));

						assert debug("ALOAD 4");
						mv.visitVarInsn(ALOAD, 4);

						if (value != null & returnType != value.getClass()) {
							dataConversion(returnType);
							checkcast(returnType);
						}

						assert debug("INVOKEINTERFACE Map.put");
						mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "put",
								"(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");

						assert debug("POP");
						mv.visitInsn(POP);

						assert debug("ALOAD 4");
						mv.visitVarInsn(ALOAD, 4);
					}
				} else if (ctx instanceof List) {
					if (MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING
							&& hasPropertyHandler(List.class)) {
						propHandlerByteCodePut(ex, ctx, List.class, value);
					} else {
						// noinspection unchecked
						((List) ctx).set(eval(ex, ctx, variableFactory, Integer.class),
								convert(value, returnType = verifier.analyze()));

						writeLiteralOrSubexpression(subCompileExpression(ex.toCharArray(), pCtx));
						unwrapPrimitive(int.class);

						assert debug("ALOAD 4");
						mv.visitVarInsn(ALOAD, 4);

						if (value != null & !value.getClass().isAssignableFrom(returnType)) {
							dataConversion(returnType);
							checkcast(returnType);
						}

						assert debug("INVOKEINTERFACE List.set");
						mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "set",
								"(ILjava/lang/Object;)Ljava/lang/Object;");

						assert debug("ALOAD 4");
						mv.visitVarInsn(ALOAD, 4);
					}
				} else if (MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING
						&& hasPropertyHandler(ctx.getClass())) {
					propHandlerByteCodePut(ex, ctx, ctx.getClass(), value);
				} else if (ctx.getClass().isArray()) {
					if (MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING
							&& hasPropertyHandler(Array.class)) {
						propHandlerByteCodePut(ex, ctx, Array.class, value);
					} else {
						final Class type = getBaseComponentType(ctx.getClass());

						Object idx = eval(ex, ctx, variableFactory);

						writeLiteralOrSubexpression(subCompileExpression(ex.toCharArray(), pCtx),
								int.class);
						if (!(idx instanceof Integer)) {
							dataConversion(Integer.class);
							idx = DataConversion.convert(idx, Integer.class);
							unwrapPrimitive(int.class);
						}

						assert debug("ALOAD 4");
						mv.visitVarInsn(ALOAD, 4);

						if (type.isPrimitive()) {
							unwrapPrimitive(type);
						} else if (!type.equals(value.getClass())) {
							dataConversion(type);
						}

						arrayStore(type);

						// noinspection unchecked
						Array.set(ctx, (Integer) idx, convert(value, type));

						assert debug("ALOAD 4");
						mv.visitVarInsn(ALOAD, 4);
					}
				} else {
					throw new PropertyAccessException("cannot bind to collection property: "
							+ new String(expr) + ": not a recognized collection type: " + ctx.getClass(),
							expr, start);
				}

				deferFinish = false;
				noinit = false;

				_finishJIT();

				try {
					deferFinish = false;
					return _initializeAccessor();
				} catch (final Exception e) {
					throw new CompileException("could not generate accessor", expr, start, e);
				}
			}

			final String tk = new String(expr, this.cursor, this.length);
			final Member member = getFieldOrWriteAccessor(ctx.getClass(), tk, value == null ? null
					: ingressType);

			if (GlobalListenerFactory.hasSetListeners()) {
				mv.visitVarInsn(ALOAD, 1);
				mv.visitLdcInsn(tk);
				mv.visitVarInsn(ALOAD, 3);
				mv.visitVarInsn(ALOAD, 4);
				mv.visitMethodInsn(INVOKESTATIC, NAMESPACE + "integration/GlobalListenerFactory",
						"notifySetListeners", "(Ljava/lang/Object;Ljava/lang/String;L" + NAMESPACE
								+ "integration/VariableResolverFactory;Ljava/lang/Object;)V");

				GlobalListenerFactory.notifySetListeners(ctx, tk, variableFactory, value);
			}

			if (member instanceof Field) {
				checkcast(ctx.getClass());

				final Field fld = (Field) member;

				Label jmp = null;
				final Label jmp2 = new Label();

				if (fld.getType().isPrimitive()) {
					assert debug("ASTORE 5");
					mv.visitVarInsn(ASTORE, 5);

					assert debug("ALOAD 4");
					mv.visitVarInsn(ALOAD, 4);

					if (value == null) {
						value = PropertyTools.getPrimitiveInitialValue(fld.getType());
					}

					jmp = new Label();
					assert debug("IFNOTNULL jmp");
					mv.visitJumpInsn(IFNONNULL, jmp);

					assert debug("ALOAD 5");
					mv.visitVarInsn(ALOAD, 5);

					assert debug("ICONST_0");
					mv.visitInsn(ICONST_0);

					assert debug("PUTFIELD " + getInternalName(fld.getDeclaringClass()) + "." + tk);
					mv.visitFieldInsn(PUTFIELD, getInternalName(fld.getDeclaringClass()), tk,
							getDescriptor(fld.getType()));

					assert debug("GOTO jmp2");
					mv.visitJumpInsn(GOTO, jmp2);

					assert debug("jmp:");
					mv.visitLabel(jmp);

					assert debug("ALOAD 5");
					mv.visitVarInsn(ALOAD, 5);

					assert debug("ALOAD 4");
					mv.visitVarInsn(ALOAD, 4);

					unwrapPrimitive(fld.getType());
				} else {
					assert debug("ALOAD 4");
					mv.visitVarInsn(ALOAD, 4);
					checkcast(fld.getType());
				}

				if (jmp == null && value != null && !fld.getType().isAssignableFrom(value.getClass())) {
					if (!canConvert(fld.getType(), value.getClass())) {
						throw new CompileException("cannot convert type: " + value.getClass() + ": to "
								+ fld.getType(), expr, start);
					}

					dataConversion(fld.getType());
					fld.set(ctx, convert(value, fld.getType()));
				} else {
					fld.set(ctx, value);
				}

				assert debug("PUTFIELD " + getInternalName(fld.getDeclaringClass()) + "." + tk);
				mv.visitFieldInsn(PUTFIELD, getInternalName(fld.getDeclaringClass()), tk,
						getDescriptor(fld.getType()));

				assert debug("jmp2:");
				mv.visitLabel(jmp2);

				assert debug("ALOAD 4");
				mv.visitVarInsn(ALOAD, 4);

			} else if (member != null) {
				assert debug("CHECKCAST " + getInternalName(ctx.getClass()));
				mv.visitTypeInsn(CHECKCAST, getInternalName(ctx.getClass()));

				final Method meth = (Method) member;

				assert debug("ALOAD 4");
				mv.visitVarInsn(ALOAD, 4);

				final Class targetType = meth.getParameterTypes()[0];

				Label jmp = null;
				final Label jmp2 = new Label();
				if (value != null && !targetType.isAssignableFrom(value.getClass())) {
					if (!canConvert(targetType, value.getClass())) {
						throw new CompileException("cannot convert type: " + value.getClass() + ": to "
								+ meth.getParameterTypes()[0], expr, start);
					}

					dataConversion(getWrapperClass(targetType));
					if (targetType.isPrimitive()) {
						unwrapPrimitive(targetType);
					} else {
						checkcast(targetType);
					}
					meth.invoke(ctx, convert(value, meth.getParameterTypes()[0]));
				} else {
					if (targetType.isPrimitive()) {

						if (value == null) {
							value = PropertyTools.getPrimitiveInitialValue(targetType);
						}

						jmp = new Label();
						assert debug("IFNOTNULL jmp");
						mv.visitJumpInsn(IFNONNULL, jmp);

						assert debug("ICONST_0");
						mv.visitInsn(ICONST_0);

						assert debug("INVOKEVIRTUAL " + getInternalName(meth.getDeclaringClass()) + "."
								+ meth.getName());
						mv.visitMethodInsn(INVOKEVIRTUAL, getInternalName(meth.getDeclaringClass()),
								meth.getName(), getMethodDescriptor(meth));

						assert debug("GOTO jmp2");
						mv.visitJumpInsn(GOTO, jmp2);

						assert debug("jmp:");
						mv.visitLabel(jmp);

						assert debug("ALOAD 4");
						mv.visitVarInsn(ALOAD, 4);

						unwrapPrimitive(targetType);
					} else {
						checkcast(targetType);
					}

					meth.invoke(ctx, value);

				}

				assert debug("INVOKEVIRTUAL " + getInternalName(meth.getDeclaringClass()) + "."
						+ meth.getName());
				mv.visitMethodInsn(INVOKEVIRTUAL, getInternalName(meth.getDeclaringClass()),
						meth.getName(), getMethodDescriptor(meth));

				assert debug("jmp2:");
				mv.visitLabel(jmp2);

				assert debug("ALOAD 4");
				mv.visitVarInsn(ALOAD, 4);
			} else if (ctx instanceof Map) {
				assert debug("CHECKCAST " + getInternalName(ctx.getClass()));
				mv.visitTypeInsn(CHECKCAST, getInternalName(ctx.getClass()));

				assert debug("LDC '" + tk + "'");
				mv.visitLdcInsn(tk);

				assert debug("ALOAD 4");
				mv.visitVarInsn(ALOAD, 4);

				assert debug("INVOKEVIRTUAL java/util/HashMap.put");
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/HashMap", "put",
						"(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");

				assert debug("ALOAD 4");
				mv.visitVarInsn(ALOAD, 4);

				// noinspection unchecked
				((Map) ctx).put(tk, value);
			} else {
				throw new PropertyAccessException("could not access property (" + tk + ") in: "
						+ ingressType.getName(), expr, start);
			}
		} catch (final InvocationTargetException e) {
			throw new PropertyAccessException("could not access property", expr, start, e);
		} catch (final IllegalAccessException e) {
			throw new PropertyAccessException("could not access property", expr, start, e);
		}

		try {
			deferFinish = false;
			noinit = false;

			_finishJIT();
			return _initializeAccessor();
		} catch (final Exception e) {

			throw new CompileException("could not generate accessor", expr, start, e);
		}
	}

	private void _finishJIT() {
		if (deferFinish) {
			return;
		}

		if (returnType != null && returnType.isPrimitive()) {
			// noinspection unchecked
			wrapPrimitive(returnType);
		}

		if (returnType == void.class) {
			assert debug("ACONST_NULL");
			mv.visitInsn(ACONST_NULL);
		}

		assert debug("ARETURN");
		mv.visitInsn(ARETURN);

		assert debug("\n{METHOD STATS (maxstack=" + stacksize + ")}\n");

		dumpAdvancedDebugging(); // dump advanced debugging if necessary

		mv.visitMaxs(stacksize, maxlocals);
		mv.visitEnd();

		mv = cw.visitMethod(ACC_PUBLIC, "getKnownEgressType", "()Ljava/lang/Class;", null, null);
		mv.visitCode();
		visitConstantClass(returnType);
		mv.visitInsn(ARETURN);

		mv.visitMaxs(1, 1);
		mv.visitEnd();

		if (propNull) {
			cw.visitField(ACC_PUBLIC, "nullPropertyHandler",
					"L" + NAMESPACE + "integration/PropertyHandler;", null, null).visitEnd();
		}

		if (methNull) {
			cw.visitField(ACC_PUBLIC, "nullMethodHandler",
					"L" + NAMESPACE + "integration/PropertyHandler;", null, null).visitEnd();
		}

		buildInputs();

		if (buildLog != null && buildLog.length() != 0 && expr != null) {
			mv = cw.visitMethod(ACC_PUBLIC, "toString", "()Ljava/lang/String;", null, null);
			mv.visitCode();
			final Label l0 = new Label();
			mv.visitLabel(l0);
			mv.visitLdcInsn(buildLog.toString() + "\n\n## { " + new String(expr) + " }");
			mv.visitInsn(ARETURN);
			final Label l1 = new Label();
			mv.visitLabel(l1);
			mv.visitMaxs(1, 1);
			mv.visitEnd();
		}

		cw.visitEnd();

	}

	private void visitConstantClass(Class<?> clazz) {
		if (clazz == null) {
			clazz = Object.class;
		}
		if (clazz.isPrimitive()) {
			mv.visitFieldInsn(GETSTATIC, toNonPrimitiveType(clazz).getName().replace(".", "/"),
					"TYPE", "Ljava/lang/Class;");
		} else {
			mv.visitLdcInsn(net.simpleframework.lib.org.objectweb.asm.Type.getType(clazz));
		}
	}

	private Accessor _initializeAccessor() throws Exception {
		if (deferFinish) {
			return null;
		}
		/**
		 * Hot load the class we just generated.
		 */
		final Class cls = loadClass(className, cw.toByteArray());

		assert debug("[MVEL JIT Completed Optimization <<" + (expr != null ? new String(expr) : "")
				+ ">>]::" + cls + " (time: " + (System.currentTimeMillis() - time) + "ms)");

		Object o;

		try {
			if (compiledInputs.size() == 0) {
				o = cls.newInstance();
			} else {
				final Class[] parms = new Class[compiledInputs.size()];
				for (int i = 0; i < compiledInputs.size(); i++) {
					parms[i] = ExecutableStatement.class;
				}
				o = cls.getConstructor(parms).newInstance(
						compiledInputs.toArray(new ExecutableStatement[compiledInputs.size()]));
			}

			if (propNull) {
				cls.getField("nullPropertyHandler").set(o, getNullPropertyHandler());
			}
			if (methNull) {
				cls.getField("nullMethodHandler").set(o, getNullMethodHandler());
			}

		} catch (final VerifyError e) {
			System.out
					.println("**** COMPILER BUG! REPORT THIS IMMEDIATELY AT http://jira.codehaus.org/browse/mvel2");
			System.out.println("Expression: " + (expr == null ? null : new String(expr)));
			throw e;
		}

		return (Accessor) o;
	}

	private Accessor compileAccessor() {
		assert debug("<<INITIATE COMPILE>>");

		Object curr = ctx;

		try {
			if (!MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING) {
				while (cursor < end) {
					switch (nextSubToken()) {
					case BEAN:
						curr = getBeanProperty(curr, capture());
						break;
					case METH:
						curr = getMethod(curr, capture());
						break;
					case COL:
						curr = getCollectionProperty(curr, capture());
						break;
					case WITH:
						curr = getWithProperty(curr);
						break;
					}

					// check to see if a null safety is enabled on this property.
					if (fields == -1) {
						if (curr == null) {
							if (nullSafe) {
								throw new OptimizationNotSupported();
							}
							break;
						} else {
							fields = 0;
						}
					}

					first = false;

					if (nullSafe && cursor < end) {

						assert debug("DUP");
						mv.visitInsn(DUP);

						final Label j = new Label();

						assert debug("IFNONNULL : jump");
						mv.visitJumpInsn(IFNONNULL, j);

						assert debug("ARETURN");
						mv.visitInsn(ARETURN);

						assert debug("LABEL:jump");
						mv.visitLabel(j);
					}
				}
			} else {
				while (cursor < end) {
					switch (nextSubToken()) {
					case BEAN:
						curr = getBeanPropertyAO(curr, capture());
						break;
					case METH:
						curr = getMethod(curr, capture());
						break;
					case COL:
						curr = getCollectionPropertyAO(curr, capture());
						break;
					case WITH:
						curr = getWithProperty(curr);
						break;
					}

					// check to see if a null safety is enabled on this property.
					if (fields == -1) {
						if (curr == null) {
							if (nullSafe) {
								throw new OptimizationNotSupported();
							}
							break;
						} else {
							fields = 0;
						}
					}

					first = false;

					if (nullSafe && cursor < end) {

						assert debug("DUP");
						mv.visitInsn(DUP);

						final Label j = new Label();

						assert debug("IFNONNULL : jump");
						mv.visitJumpInsn(IFNONNULL, j);

						assert debug("ARETURN");
						mv.visitInsn(ARETURN);

						assert debug("LABEL:jump");
						mv.visitLabel(j);
					}
				}
			}

			val = curr;

			_finishJIT();

			return _initializeAccessor();
		} catch (final InvocationTargetException e) {
			throw new PropertyAccessException(new String(expr), expr, st, e);
		} catch (final IllegalAccessException e) {
			throw new PropertyAccessException(new String(expr), expr, st, e);
		} catch (final IndexOutOfBoundsException e) {
			throw new PropertyAccessException(new String(expr), expr, st, e);
		} catch (final PropertyAccessException e) {
			throw new CompileException(e.getMessage(), expr, st, e);
		} catch (final CompileException e) {
			throw e;
		} catch (final NullPointerException e) {
			throw new PropertyAccessException(new String(expr), expr, st, e);
		} catch (final OptimizationNotSupported e) {
			throw e;
		} catch (final Exception e) {
			throw new CompileException(e.getMessage(), expr, st, e);
		}
	}

	private Object getWithProperty(final Object ctx) {
		assert debug("\n  ** ENTER -> {with}");

		if (first) {
			assert debug("ALOAD 1");
			mv.visitVarInsn(ALOAD, 1);
			first = false;
		}

		final String root = new String(expr, 0, cursor - 1).trim();

		final int start = cursor + 1;
		cursor = balancedCaptureWithLineAccounting(expr, cursor, end, '{', pCtx);
		this.returnType = ctx != null ? ctx.getClass() : null;

		for (final WithNode.ParmValuePair aPvp : WithNode.compileWithExpressions(expr, start,
				cursor++ - start, root, ingressType, pCtx)) {
			assert debug("DUP");
			mv.visitInsn(DUP);

			assert debug("ASTORE " + (5 + compileDepth) + " (withctx)");
			mv.visitVarInsn(ASTORE, 5 + compileDepth);

			aPvp.eval(ctx, variableFactory);

			if (aPvp.getSetExpression() == null) {
				addSubstatement(aPvp.getStatement());
			} else {
				compiledInputs.add((ExecutableStatement) aPvp.getSetExpression());

				// load set expression
				assert debug("ALOAD 0");
				mv.visitVarInsn(ALOAD, 0);

				assert debug("GETFIELD p" + (compiledInputs.size() - 1));
				mv.visitFieldInsn(GETFIELD, className, "p" + (compiledInputs.size() - 1), "L"
						+ NAMESPACE + "compiler/ExecutableStatement;");

				// ctx
				assert debug("ALOAD " + (5 + compileDepth) + "(withctx)");
				mv.visitVarInsn(ALOAD, (5 + compileDepth));

				// elCtx
				assert debug("ALOAD 2");
				mv.visitVarInsn(ALOAD, 2);

				// variable factory
				assert debug("ALOAD 3");
				mv.visitVarInsn(ALOAD, 3);

				// the value to set.
				addSubstatement(aPvp.getStatement());

				assert debug("INVOKEINTERFACE Accessor.setValue");
				mv.visitMethodInsn(
						INVOKEINTERFACE,
						NAMESPACE + "compiler/ExecutableStatement",
						"setValue",
						"(Ljava/lang/Object;Ljava/lang/Object;L"
								+ NAMESPACE
								+ "integration/VariableResolverFactory;Ljava/lang/Object;)Ljava/lang/Object;");

				assert debug("POP");
				mv.visitInsn(POP);
			}
		}

		return ctx;
	}

	private Object getBeanPropertyAO(final Object ctx, final String property)
			throws IllegalAccessException, InvocationTargetException {
		if (ctx != null && hasPropertyHandler(ctx.getClass())) {
			return propHandlerByteCode(property, ctx, ctx.getClass());
		}
		return getBeanProperty(ctx, property);
	}

	private Object getBeanProperty(final Object ctx, final String property)
			throws IllegalAccessException, InvocationTargetException {

		assert debug("\n  **  ENTER -> {bean: " + property + "; ctx=" + ctx + "}");

		if ((pCtx == null ? currType : pCtx.getVarOrInputTypeOrNull(property)) == Object.class
				&& !pCtx.isStrongTyping()) {
			currType = null;
		}

		if (returnType != null && returnType.isPrimitive()) {
			// noinspection unchecked
			wrapPrimitive(returnType);
		}

		boolean classRef = false;

		Class<?> cls;
		if (ctx instanceof Class) {
			if (MVEL.COMPILER_OPT_SUPPORT_JAVA_STYLE_CLASS_LITERALS && "class".equals(property)) {
				ldcClassConstant((Class<?>) ctx);

				return ctx;
			}

			cls = (Class<?>) ctx;
			classRef = true;
		} else if (ctx != null) {
			cls = ctx.getClass();
		} else {
			cls = null;
		}

		if (hasPropertyHandler(cls)) {
			final PropertyHandler prop = getPropertyHandler(cls);
			if (prop instanceof ProducesBytecode) {
				((ProducesBytecode) prop).produceBytecodeGet(mv, property, variableFactory);
				return prop.getProperty(property, ctx, variableFactory);
			} else {
				throw new RuntimeException(
						"unable to compileShared: custom accessor does not support producing bytecode: "
								+ prop.getClass().getName());
			}
		}

		Member member = cls != null ? getFieldOrAccessor(cls, property) : null;

		if (member != null && classRef && (member.getModifiers() & Modifier.STATIC) == 0) {
			member = null;
		}

		if (member != null && hasGetListeners()) {
			mv.visitVarInsn(ALOAD, 1);
			mv.visitLdcInsn(member.getName());
			mv.visitVarInsn(ALOAD, 3);
			mv.visitMethodInsn(INVOKESTATIC, NAMESPACE + "integration/GlobalListenerFactory",
					"notifyGetListeners", "(Ljava/lang/Object;Ljava/lang/String;L" + NAMESPACE
							+ "integration/VariableResolverFactory;)V");

			notifyGetListeners(ctx, member.getName(), variableFactory);
		}

		if (first) {
			if ("this".equals(property)) {
				assert debug("ALOAD 2");
				mv.visitVarInsn(ALOAD, 2);
				return thisRef;
			} else if (variableFactory != null && variableFactory.isResolveable(property)) {

				if (variableFactory.isIndexedFactory() && variableFactory.isTarget(property)) {
					int idx;
					try {
						loadVariableByIndex(idx = variableFactory.variableIndexOf(property));
					} catch (final Exception e) {
						throw new OptimizationFailure(property);
					}

					return variableFactory.getIndexedVariableResolver(idx).getValue();
				} else {
					try {
						loadVariableByName(property);
					} catch (final Exception e) {
						throw new OptimizationFailure("critical error in JIT", e);
					}

					return variableFactory.getVariableResolver(property).getValue();
				}
			} else {
				assert debug("ALOAD 1");
				mv.visitVarInsn(ALOAD, 1);
			}
		}

		if (member instanceof Field) {
			Object o = ((Field) member).get(ctx);

			if (((member.getModifiers() & STATIC) != 0)) {
				// Check if the static field reference is a constant and a
				// primitive.
				if ((member.getModifiers() & FINAL) != 0
						&& (o instanceof String || ((Field) member).getType().isPrimitive())) {
					o = ((Field) member).get(null);
					assert debug("LDC " + valueOf(o));
					mv.visitLdcInsn(o);
					wrapPrimitive(o.getClass());

					if (hasNullPropertyHandler()) {
						if (o == null) {
							o = getNullPropertyHandler().getProperty(member.getName(), ctx,
									variableFactory);
						}

						writeOutNullHandler(member, 0);
					}
					return o;
				} else {
					assert debug("GETSTATIC " + getDescriptor(member.getDeclaringClass()) + "."
							+ member.getName() + "::" + getDescriptor(((Field) member).getType()));

					mv.visitFieldInsn(GETSTATIC, getInternalName(member.getDeclaringClass()),
							member.getName(), getDescriptor(returnType = ((Field) member).getType()));
				}
			} else {
				assert debug("CHECKCAST " + getInternalName(cls));
				mv.visitTypeInsn(CHECKCAST, getInternalName(cls));

				assert debug("GETFIELD " + property + ":" + getDescriptor(((Field) member).getType()));
				mv.visitFieldInsn(GETFIELD, getInternalName(cls), property,
						getDescriptor(returnType = ((Field) member).getType()));
			}

			returnType = ((Field) member).getType();

			if (hasNullPropertyHandler()) {
				if (o == null) {
					o = getNullPropertyHandler().getProperty(member.getName(), ctx, variableFactory);
				}

				writeOutNullHandler(member, 0);
			}

			currType = toNonPrimitiveType(returnType);
			return o;
		} else if (member != null) {
			Object o;

			if (first) {
				assert debug("ALOAD 1 (B)");
				mv.visitVarInsn(ALOAD, 1);
			}

			try {
				o = ((Method) member).invoke(ctx, EMPTYARG);

				if (returnType != member.getDeclaringClass()) {
					assert debug("CHECKCAST " + getInternalName(member.getDeclaringClass()));
					mv.visitTypeInsn(CHECKCAST, getInternalName(member.getDeclaringClass()));
				}

				returnType = ((Method) member).getReturnType();

				assert debug("INVOKEVIRTUAL " + member.getName() + ":" + returnType);
				mv.visitMethodInsn(INVOKEVIRTUAL, getInternalName(member.getDeclaringClass()),
						member.getName(), getMethodDescriptor((Method) member));
			} catch (final IllegalAccessException e) {
				final Method iFaceMeth = determineActualTargetMethod((Method) member);
				if (iFaceMeth == null) {
					throw new PropertyAccessException("could not access field: " + cls.getName() + "."
							+ property, expr, st, e);
				}

				assert debug("CHECKCAST " + getInternalName(iFaceMeth.getDeclaringClass()));
				mv.visitTypeInsn(CHECKCAST, getInternalName(iFaceMeth.getDeclaringClass()));

				returnType = iFaceMeth.getReturnType();

				assert debug("INVOKEINTERFACE " + member.getName() + ":" + returnType);
				mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(iFaceMeth.getDeclaringClass()),
						member.getName(), getMethodDescriptor((Method) member));

				o = iFaceMeth.invoke(ctx, EMPTYARG);
			} catch (final IllegalArgumentException e) {
				if (member.getDeclaringClass().equals(ctx)) {
					try {
						final Class c = Class.forName(member.getDeclaringClass().getName() + "$"
								+ property);

						throw new CompileException("name collision between innerclass: "
								+ c.getCanonicalName() + "; and bean accessor: " + property + " ("
								+ member.toString() + ")", expr, tkStart);
					} catch (final ClassNotFoundException e2) {
						// fallthru
					}
				}
				throw e;
			}

			if (hasNullPropertyHandler()) {
				if (o == null) {
					o = getNullPropertyHandler().getProperty(member.getName(), ctx, variableFactory);
				}
				writeOutNullHandler(member, 0);
			}

			currType = toNonPrimitiveType(returnType);
			return o;
		} else if (ctx instanceof Map && (((Map) ctx).containsKey(property) || nullSafe)) {
			assert debug("CHECKCAST java/util/Map");
			mv.visitTypeInsn(CHECKCAST, "java/util/Map");

			assert debug("LDC: \"" + property + "\"");
			mv.visitLdcInsn(property);

			assert debug("INVOKEINTERFACE: get");
			mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "get",
					"(Ljava/lang/Object;)Ljava/lang/Object;");
			return ((Map) ctx).get(property);
		} else if (first && "this".equals(property)) {
			assert debug("ALOAD 2");
			mv.visitVarInsn(ALOAD, 2); // load the thisRef value.

			return this.thisRef;
		} else if ("length".equals(property) && ctx.getClass().isArray()) {
			anyArrayCheck(ctx.getClass());

			assert debug("ARRAYLENGTH");
			mv.visitInsn(ARRAYLENGTH);

			wrapPrimitive(int.class);
			return getLength(ctx);
		} else if (LITERALS.containsKey(property)) {
			final Object lit = LITERALS.get(property);

			if (lit instanceof Class) {
				ldcClassConstant((Class) lit);
			}

			return lit;
		} else {
			final Object ts = tryStaticAccess();

			if (ts != null) {
				if (ts instanceof Class) {
					ldcClassConstant((Class) ts);
					return ts;
				} else if (ts instanceof Method) {
					writeFunctionPointerStub(((Method) ts).getDeclaringClass(), (Method) ts);
					return ts;
				} else {
					final Field f = (Field) ts;

					if ((f.getModifiers() & FINAL) != 0) {
						final Object finalVal = f.get(null);
						assert debug("LDC " + valueOf(finalVal));
						mv.visitLdcInsn(finalVal);
						wrapPrimitive(finalVal.getClass());
						return finalVal;
					} else {
						assert debug("GETSTATIC " + getInternalName(f.getDeclaringClass()) + "."
								+ ((Field) ts).getName() + "::" + getDescriptor(f.getType()));

						mv.visitFieldInsn(GETSTATIC, getInternalName(f.getDeclaringClass()), f.getName(),
								getDescriptor(returnType = f.getType()));

						return f.get(null);
					}
				}
			} else if (ctx instanceof Class) {
				/**
				 * This is our ugly support for function pointers. This works but
				 * needs to be re-thought out at some point.
				 */
				final Class c = (Class) ctx;
				for (final Method m : c.getMethods()) {
					if (property.equals(m.getName())) {
						if (MVEL.COMPILER_OPT_ALLOW_NAKED_METH_CALL) {
							assert debug("POP");
							mv.visitInsn(POP);
							assert debug("INVOKESTATIC " + m.getName());
							mv.visitMethodInsn(INVOKESTATIC, getInternalName(m.getDeclaringClass()),
									m.getName(), getMethodDescriptor(m));

							returnType = m.getReturnType();

							return m.invoke(null, EMPTY_OBJ_ARR);
						} else {
							writeFunctionPointerStub(c, m);
							return m;
						}
					}
				}

				try {
					final Class subClass = findClass(variableFactory, c.getName() + "$" + property, pCtx);
					ldcClassConstant(subClass);
					return subClass;
				} catch (final ClassNotFoundException cnfe) {
					// fall through.
				}

			} else if (MVEL.COMPILER_OPT_ALLOW_NAKED_METH_CALL) {
				return getMethod(ctx, property);
			}

			if (ctx == null) {
				throw new PropertyAccessException("unresolvable property or identifier: " + property,
						expr, st);
			} else {
				throw new PropertyAccessException("could not access: " + property + "; in class: "
						+ ctx.getClass().getName(), expr, st);
			}
		}
	}

	private void writeFunctionPointerStub(final Class c, final Method m) {
		ldcClassConstant(c);

		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getMethods",
				"()[Ljava/lang/reflect/Method;");
		mv.visitVarInsn(ASTORE, 7);
		mv.visitInsn(ICONST_0);
		mv.visitVarInsn(ISTORE, 5);
		mv.visitVarInsn(ALOAD, 7);
		mv.visitInsn(ARRAYLENGTH);
		mv.visitVarInsn(ISTORE, 6);
		final Label l1 = new Label();
		mv.visitJumpInsn(GOTO, l1);
		final Label l2 = new Label();
		mv.visitLabel(l2);
		mv.visitVarInsn(ALOAD, 7);
		mv.visitVarInsn(ILOAD, 5);
		mv.visitInsn(AALOAD);
		mv.visitVarInsn(ASTORE, 4);
		final Label l3 = new Label();
		mv.visitLabel(l3);
		mv.visitLdcInsn(m.getName());
		mv.visitVarInsn(ALOAD, 4);
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Method", "getName",
				"()Ljava/lang/String;");
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z");
		final Label l4 = new Label();
		mv.visitJumpInsn(IFEQ, l4);
		final Label l5 = new Label();
		mv.visitLabel(l5);
		mv.visitVarInsn(ALOAD, 4);
		mv.visitInsn(ARETURN);
		mv.visitLabel(l4);
		mv.visitIincInsn(5, 1);
		mv.visitLabel(l1);
		mv.visitVarInsn(ILOAD, 5);
		mv.visitVarInsn(ILOAD, 6);
		mv.visitJumpInsn(IF_ICMPLT, l2);
		final Label l6 = new Label();
		mv.visitLabel(l6);
		mv.visitInsn(ACONST_NULL);
		mv.visitInsn(ARETURN);

		// deferFinish = true;
	}

	private Object getCollectionProperty(Object ctx, final String prop)
			throws IllegalAccessException, InvocationTargetException {
		if (prop.trim().length() > 0) {
			ctx = getBeanProperty(ctx, prop);
			first = false;
		}

		currType = null;
		if (ctx == null) {
			return null;
		}

		assert debug("\n  **  ENTER -> {collection:<<" + prop + ">>; ctx=" + ctx + "}");

		if (first) {
			assert debug("ALOAD 1");
			mv.visitVarInsn(ALOAD, 1);
		}

		final int start = ++cursor;

		skipWhitespace();

		if (cursor == end) {
			throw new CompileException("unterminated '['", expr, st);
		}

		if (scanTo(']')) {
			throw new CompileException("unterminated '['", expr, st);
		}

		final String tk = new String(expr, start, cursor - start);

		assert debug("{collection token: [" + tk + "]}");

		final ExecutableStatement compiled = (ExecutableStatement) subCompileExpression(
				tk.toCharArray(), pCtx);
		final Object item = compiled.getValue(ctx, variableFactory);

		++cursor;

		if (ctx instanceof Map) {
			assert debug("CHECKCAST java/util/Map");
			mv.visitTypeInsn(CHECKCAST, "java/util/Map");

			final Class c = writeLiteralOrSubexpression(compiled);
			if (c != null && c.isPrimitive()) {
				wrapPrimitive(c);
			}

			assert debug("INVOKEINTERFACE: get");
			mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "get",
					"(Ljava/lang/Object;)Ljava/lang/Object;");

			return ((Map) ctx).get(item);
		} else if (ctx instanceof List) {
			assert debug("CHECKCAST java/util/List");
			mv.visitTypeInsn(CHECKCAST, "java/util/List");

			writeLiteralOrSubexpression(compiled, int.class);

			assert debug("INVOKEINTERFACE: java/util/List.get");
			mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "get", "(I)Ljava/lang/Object;");

			return ((List) ctx).get(convert(item, Integer.class));

		} else if (ctx.getClass().isArray()) {
			assert debug("CHECKCAST " + getDescriptor(ctx.getClass()));
			mv.visitTypeInsn(CHECKCAST, getDescriptor(ctx.getClass()));

			writeLiteralOrSubexpression(compiled, int.class, item.getClass());

			final Class cls = getBaseComponentType(ctx.getClass());
			if (cls.isPrimitive()) {
				if (cls == int.class) {
					assert debug("IALOAD");
					mv.visitInsn(IALOAD);
				} else if (cls == char.class) {
					assert debug("CALOAD");
					mv.visitInsn(CALOAD);
				} else if (cls == boolean.class) {
					assert debug("BALOAD");
					mv.visitInsn(BALOAD);
				} else if (cls == double.class) {
					assert debug("DALOAD");
					mv.visitInsn(DALOAD);
				} else if (cls == float.class) {
					assert debug("FALOAD");
					mv.visitInsn(FALOAD);
				} else if (cls == short.class) {
					assert debug("SALOAD");
					mv.visitInsn(SALOAD);
				} else if (cls == long.class) {
					assert debug("LALOAD");
					mv.visitInsn(LALOAD);
				} else if (cls == byte.class) {
					assert debug("BALOAD");
					mv.visitInsn(BALOAD);
				}

				wrapPrimitive(cls);
			} else {
				assert debug("AALOAD");
				mv.visitInsn(AALOAD);
			}

			return Array.get(ctx, convert(item, Integer.class));
		} else if (ctx instanceof CharSequence) {
			assert debug("CHECKCAST java/lang/CharSequence");
			mv.visitTypeInsn(CHECKCAST, "java/lang/CharSequence");

			if (item instanceof Integer) {
				intPush((Integer) item);

				assert debug("INVOKEINTERFACE java/lang/CharSequence.charAt");
				mv.visitMethodInsn(INVOKEINTERFACE, "java/lang/CharSequence", "charAt", "(I)C");

				wrapPrimitive(char.class);

				return ((CharSequence) ctx).charAt((Integer) item);
			} else {
				writeLiteralOrSubexpression(compiled, Integer.class);
				unwrapPrimitive(int.class);

				assert debug("INVOKEINTERFACE java/lang/CharSequence.charAt");
				mv.visitMethodInsn(INVOKEINTERFACE, "java/lang/CharSequence", "charAt", "(I)C");

				wrapPrimitive(char.class);

				return ((CharSequence) ctx).charAt(convert(item, Integer.class));
			}
		} else {
			final TypeDescriptor tDescr = new TypeDescriptor(expr, this.start, length, 0);
			if (tDescr.isArray()) {
				try {
					final Class cls = getClassReference((Class) ctx, tDescr, variableFactory, pCtx);
					// rootNode = new StaticReferenceAccessor(cls);
					ldcClassConstant(cls);
					return cls;
				} catch (final Exception e) {
					// fall through
				}
			}

			throw new CompileException("illegal use of []: unknown type: "
					+ (ctx == null ? null : ctx.getClass().getName()), expr, st);
		}
	}

	private Object getCollectionPropertyAO(Object ctx, final String prop)
			throws IllegalAccessException, InvocationTargetException {
		if (prop.length() > 0) {
			ctx = getBeanProperty(ctx, prop);
			first = false;
		}

		currType = null;
		if (ctx == null) {
			return null;
		}

		assert debug("\n  **  ENTER -> {collection:<<" + prop + ">>; ctx=" + ctx + "}");

		final int _start = ++cursor;

		skipWhitespace();

		if (cursor == end) {
			throw new CompileException("unterminated '['", expr, st);
		}

		if (scanTo(']')) {
			throw new CompileException("unterminated '['", expr, st);
		}

		final String tk = new String(expr, _start, cursor - _start);

		assert debug("{collection token:<<" + tk + ">>}");

		final ExecutableStatement compiled = (ExecutableStatement) subCompileExpression(tk
				.toCharArray());
		final Object item = compiled.getValue(ctx, variableFactory);

		++cursor;

		if (ctx instanceof Map) {
			if (hasPropertyHandler(Map.class)) {
				return propHandlerByteCode(tk, ctx, Map.class);
			} else {
				if (first) {
					assert debug("ALOAD 1");
					mv.visitVarInsn(ALOAD, 1);
				}

				assert debug("CHECKCAST java/util/Map");
				mv.visitTypeInsn(CHECKCAST, "java/util/Map");

				final Class c = writeLiteralOrSubexpression(compiled);
				if (c != null && c.isPrimitive()) {
					wrapPrimitive(c);
				}

				assert debug("INVOKEINTERFACE: Map.get");
				mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "get",
						"(Ljava/lang/Object;)Ljava/lang/Object;");
			}

			return ((Map) ctx).get(item);
		} else if (ctx instanceof List) {
			if (hasPropertyHandler(List.class)) {
				return propHandlerByteCode(tk, ctx, List.class);
			} else {
				if (first) {
					assert debug("ALOAD 1");
					mv.visitVarInsn(ALOAD, 1);
				}

				assert debug("CHECKCAST java/util/List");
				mv.visitTypeInsn(CHECKCAST, "java/util/List");

				writeLiteralOrSubexpression(compiled, int.class);

				assert debug("INVOKEINTERFACE: java/util/List.get");
				mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "get", "(I)Ljava/lang/Object;");

				return ((List) ctx).get(convert(item, Integer.class));
			}
		} else if (ctx.getClass().isArray()) {
			if (hasPropertyHandler(Array.class)) {
				return propHandlerByteCode(tk, ctx, Array.class);
			} else {
				if (first) {
					assert debug("ALOAD 1");
					mv.visitVarInsn(ALOAD, 1);
				}

				assert debug("CHECKCAST " + getDescriptor(ctx.getClass()));
				mv.visitTypeInsn(CHECKCAST, getDescriptor(ctx.getClass()));

				writeLiteralOrSubexpression(compiled, int.class, item.getClass());

				final Class cls = getBaseComponentType(ctx.getClass());
				if (cls.isPrimitive()) {
					if (cls == int.class) {
						assert debug("IALOAD");
						mv.visitInsn(IALOAD);
					} else if (cls == char.class) {
						assert debug("CALOAD");
						mv.visitInsn(CALOAD);
					} else if (cls == boolean.class) {
						assert debug("BALOAD");
						mv.visitInsn(BALOAD);
					} else if (cls == double.class) {
						assert debug("DALOAD");
						mv.visitInsn(DALOAD);
					} else if (cls == float.class) {
						assert debug("FALOAD");
						mv.visitInsn(FALOAD);
					} else if (cls == short.class) {
						assert debug("SALOAD");
						mv.visitInsn(SALOAD);
					} else if (cls == long.class) {
						assert debug("LALOAD");
						mv.visitInsn(LALOAD);
					} else if (cls == byte.class) {
						assert debug("BALOAD");
						mv.visitInsn(BALOAD);
					}

					wrapPrimitive(cls);
				} else {
					assert debug("AALOAD");
					mv.visitInsn(AALOAD);
				}

				return Array.get(ctx, convert(item, Integer.class));
			}
		} else if (ctx instanceof CharSequence) {
			if (hasPropertyHandler(CharSequence.class)) {
				return propHandlerByteCode(tk, ctx, CharSequence.class);
			} else {
				if (first) {
					assert debug("ALOAD 1");
					mv.visitVarInsn(ALOAD, 1);
				}

				assert debug("CHECKCAST java/lang/CharSequence");
				mv.visitTypeInsn(CHECKCAST, "java/lang/CharSequence");

				if (item instanceof Integer) {
					intPush((Integer) item);

					assert debug("INVOKEINTERFACE java/lang/CharSequence.charAt");
					mv.visitMethodInsn(INVOKEINTERFACE, "java/lang/CharSequence", "charAt", "(I)C");

					wrapPrimitive(char.class);

					return ((CharSequence) ctx).charAt((Integer) item);
				} else {
					writeLiteralOrSubexpression(compiled, Integer.class);
					unwrapPrimitive(int.class);

					assert debug("INVOKEINTERFACE java/lang/CharSequence.charAt");
					mv.visitMethodInsn(INVOKEINTERFACE, "java/lang/CharSequence", "charAt", "(I)C");

					wrapPrimitive(char.class);

					return ((CharSequence) ctx).charAt(convert(item, Integer.class));
				}
			}
		} else {
			final TypeDescriptor tDescr = new TypeDescriptor(expr, start, end - start, 0);
			if (tDescr.isArray()) {
				try {
					final Class cls = getClassReference((Class) ctx, tDescr, variableFactory, pCtx);
					// rootNode = new StaticReferenceAccessor(cls);
					ldcClassConstant(cls);
					return cls;
				} catch (final Exception e) {
					// fall through
				}
			}

			throw new CompileException("illegal use of []: unknown type: "
					+ (ctx == null ? null : ctx.getClass().getName()), expr, st);
		}
	}

	@SuppressWarnings({ "unchecked" })
	private Object getMethod(Object ctx, String name) throws IllegalAccessException,
			InvocationTargetException {
		assert debug("\n  **  {method: " + name + "}");

		final int st = cursor;
		final String tk = cursor != end && expr[cursor] == '('
				&& ((cursor = balancedCapture(expr, cursor, '(')) - st) > 1 ? new String(expr, st + 1,
				cursor - st - 1) : "";
		cursor++;

		Object[] preConvArgs;
		Object[] args;
		Class[] argTypes;
		ExecutableStatement[] es;
		List<char[]> subtokens;

		if (tk.length() == 0) {
			args = preConvArgs = ParseTools.EMPTY_OBJ_ARR;
			argTypes = ParseTools.EMPTY_CLS_ARR;
			es = null;
			subtokens = null;
		} else {
			subtokens = parseParameterList(tk.toCharArray(), 0, -1);

			es = new ExecutableStatement[subtokens.size()];
			args = new Object[subtokens.size()];
			argTypes = new Class[subtokens.size()];
			preConvArgs = new Object[es.length];

			for (int i = 0; i < subtokens.size(); i++) {
				assert debug("subtoken[" + i + "] { " + new String(subtokens.get(i)) + " }");
				preConvArgs[i] = args[i] = (es[i] = (ExecutableStatement) subCompileExpression(
						subtokens.get(i), pCtx)).getValue(this.thisRef, this.thisRef, variableFactory);

				if (es[i].isExplicitCast()) {
					argTypes[i] = es[i].getKnownEgressType();
				}
			}

			if (pCtx.isStrictTypeEnforcement()) {
				for (int i = 0; i < args.length; i++) {
					argTypes[i] = es[i].getKnownEgressType();
				}
			} else {
				for (int i = 0; i < args.length; i++) {
					if (argTypes[i] != null) {
						continue;
					}

					if (es[i].getKnownEgressType() == Object.class) {
						argTypes[i] = args[i] == null ? null : args[i].getClass();
					} else {
						argTypes[i] = es[i].getKnownEgressType();
					}
				}
			}
		}

		if (first && variableFactory != null && variableFactory.isResolveable(name)) {
			final Object ptr = variableFactory.getVariableResolver(name).getValue();

			if (ptr instanceof Method) {
				ctx = ((Method) ptr).getDeclaringClass();
				name = ((Method) ptr).getName();
			} else if (ptr instanceof MethodStub) {
				ctx = ((MethodStub) ptr).getClassReference();
				name = ((MethodStub) ptr).getMethodName();
			} else if (ptr instanceof Function) {

				if (es != null && es.length != 0) {
					compiledInputs.addAll(Arrays.asList(es));

					intPush(es.length);

					assert debug("ANEWARRAY [" + es.length + "]");
					mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");

					assert debug("ASTORE 4");
					mv.visitVarInsn(ASTORE, 4);

					for (int i = 0; i < es.length; i++) {
						assert debug("ALOAD 4");
						mv.visitVarInsn(ALOAD, 4);
						intPush(i);
						loadField(i);

						assert debug("ALOAD 1");
						mv.visitVarInsn(ALOAD, 1);

						assert debug("ALOAD 3");
						mv.visitIntInsn(ALOAD, 3);

						assert debug("INVOKEINTERFACE ExecutableStatement.getValue");
						mv.visitMethodInsn(INVOKEINTERFACE, NAMESPACE + "compiler/ExecutableStatement",
								"getValue", "(Ljava/lang/Object;L" + NAMESPACE
										+ "integration/VariableResolverFactory;)Ljava/lang/Object;");

						assert debug("AASTORE");
						mv.visitInsn(AASTORE);
					}
				} else {
					assert debug("ACONST_NULL");
					mv.visitInsn(ACONST_NULL);

					assert debug("CHECKCAST java/lang/Object");
					mv.visitTypeInsn(CHECKCAST, "[Ljava/lang/Object;");

					assert debug("ASTORE 4");
					mv.visitVarInsn(ASTORE, 4);
				}

				if (variableFactory.isIndexedFactory() && variableFactory.isTarget(name)) {
					loadVariableByIndex(variableFactory.variableIndexOf(name));
				} else {
					loadVariableByName(name);
				}

				checkcast(Function.class);

				assert debug("ALOAD 1");
				mv.visitVarInsn(ALOAD, 1);

				assert debug("ALOAD 2");
				mv.visitVarInsn(ALOAD, 2);

				assert debug("ALOAD 3");
				mv.visitVarInsn(ALOAD, 3);

				assert debug("ALOAD 4");
				mv.visitVarInsn(ALOAD, 4);

				assert debug("INVOKEVIRTUAL Function.call");
				mv.visitMethodInsn(
						INVOKEVIRTUAL,
						getInternalName(Function.class),
						"call",
						"(Ljava/lang/Object;Ljava/lang/Object;L"
								+ NAMESPACE
								+ "integration/VariableResolverFactory;[Ljava/lang/Object;)Ljava/lang/Object;");

				return ((Function) ptr).call(ctx, thisRef, variableFactory, args);
			} else {
				throw new OptimizationFailure(
						"attempt to optimize a method call for a reference that does not point to a method: "
								+ name + " (reference is type: "
								+ (ctx != null ? ctx.getClass().getName() : null) + ")");
			}

			first = false;
		} else if (returnType != null && returnType.isPrimitive()) {
			// noinspection unchecked
			wrapPrimitive(returnType);
		}

		/**
		 * If the target object is an instance of java.lang.Class itself then do
		 * not adjust the Class scope target.
		 */

		boolean classTarget = false;
		Class<?> cls = currType != null ? currType
				: ((classTarget = ctx instanceof Class) ? (Class<?>) ctx : ctx.getClass());

		currType = null;

		Method m;
		Class[] parameterTypes = null;

		/**
		 * Try to find an instance method from the class target.
		 */
		if ((m = getBestCandidate(argTypes, name, cls, cls.getMethods(), false, classTarget)) != null) {
			parameterTypes = m.getParameterTypes();
		}

		if (m == null && classTarget) {
			/**
			 * If we didn't find anything, maybe we're looking for the actual
			 * java.lang.Class methods.
			 */
			if ((m = getBestCandidate(argTypes, name, cls, Class.class.getMethods(), false)) != null) {
				parameterTypes = m.getParameterTypes();
			}
		}

		// If we didn't find anything and the declared class is different from the
		// actual one try also with the actual one
		if (m == null && cls != ctx.getClass() && !(ctx instanceof Class)) {
			cls = ctx.getClass();
			if ((m = getBestCandidate(argTypes, name, cls, cls.getMethods(), false, classTarget)) != null) {
				parameterTypes = m.getParameterTypes();
			}
		}

		if (es != null
				&& m != null
				&& m.isVarArgs()
				&& (es.length != parameterTypes.length || !(es[es.length - 1] instanceof ExecutableAccessor))) {
			// normalize ExecutableStatement for varargs
			final ExecutableStatement[] varArgEs = new ExecutableStatement[parameterTypes.length];
			final int varArgStart = parameterTypes.length - 1;
			for (int i = 0; i < varArgStart; i++) {
				varArgEs[i] = es[i];
			}

			final String varargsTypeName = parameterTypes[parameterTypes.length - 1]
					.getComponentType().getName();
			final StringBuilder sb = new StringBuilder("new ").append(varargsTypeName).append("[] {");
			for (int i = varArgStart; i < subtokens.size(); i++) {
				sb.append(subtokens.get(i));
				if (i < subtokens.size() - 1) {
					sb.append(",");
				}
			}
			final String varArgExpr = sb.append("}").toString();

			final char[] token = varArgExpr.toCharArray();
			varArgEs[varArgStart] = ((ExecutableStatement) subCompileExpression(token, pCtx));
			es = varArgEs;

			if (preConvArgs.length == parameterTypes.length - 1) {
				// empty vararg
				final Object[] preConvArgsForVarArg = new Object[parameterTypes.length];
				for (int i = 0; i < preConvArgs.length; i++) {
					preConvArgsForVarArg[i] = preConvArgs[i];
				}
				preConvArgsForVarArg[parameterTypes.length - 1] = Array.newInstance(
						parameterTypes[parameterTypes.length - 1].getComponentType(), 0);
				preConvArgs = preConvArgsForVarArg;
			}
		}

		int inputsOffset = compiledInputs.size();

		if (es != null) {
			for (final ExecutableStatement e : es) {
				if (e instanceof ExecutableLiteral) {
					continue;
				}

				compiledInputs.add(e);
			}
		}

		if (first) {
			assert debug("ALOAD 1 (D) ");
			mv.visitVarInsn(ALOAD, 1);
		}

		if (m == null) {
			final StringAppender errorBuild = new StringAppender();

			if (parameterTypes != null) {
				for (int i = 0; i < args.length; i++) {
					errorBuild.append(parameterTypes[i] != null ? parameterTypes[i].getClass().getName()
							: null);
					if (i < args.length - 1) {
						errorBuild.append(", ");
					}
				}
			}

			if ("size".equals(name) && args.length == 0 && cls.isArray()) {
				anyArrayCheck(cls);

				assert debug("ARRAYLENGTH");
				mv.visitInsn(ARRAYLENGTH);

				wrapPrimitive(int.class);
				return getLength(ctx);
			}

			throw new CompileException("unable to resolve method: " + cls.getName() + "." + name + "("
					+ errorBuild.toString() + ") [arglength=" + args.length + "]", expr, st);
		} else {
			m = getWidenedTarget(m);

			if (es != null) {
				ExecutableStatement cExpr;
				for (int i = 0; i < es.length; i++) {
					if ((cExpr = es[i]).getKnownIngressType() == null) {
						cExpr.setKnownIngressType(parameterTypes[i]);
						cExpr.computeTypeConversionRule();
					}
					if (!cExpr.isConvertableIngressEgress() && i < args.length) {
						args[i] = convert(args[i], paramTypeVarArgsSafe(parameterTypes, i, m.isVarArgs()));
					}
				}
			} else {
				/**
				 * Coerce any types if required.
				 */
				for (int i = 0; i < args.length; i++) {
					args[i] = convert(args[i], paramTypeVarArgsSafe(parameterTypes, i, m.isVarArgs()));
				}
			}

			if (m.getParameterTypes().length == 0) {
				if ((m.getModifiers() & STATIC) != 0) {
					assert debug("INVOKESTATIC " + m.getName());
					mv.visitMethodInsn(INVOKESTATIC, getInternalName(m.getDeclaringClass()),
							m.getName(), getMethodDescriptor(m));
				} else {
					assert debug("CHECKCAST " + getInternalName(m.getDeclaringClass()));
					mv.visitTypeInsn(CHECKCAST, getInternalName(m.getDeclaringClass()));

					if (m.getDeclaringClass().isInterface()) {
						assert debug("INVOKEINTERFACE " + m.getName());
						mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(m.getDeclaringClass()),
								m.getName(), getMethodDescriptor(m));

					} else {
						assert debug("INVOKEVIRTUAL " + m.getName());
						mv.visitMethodInsn(INVOKEVIRTUAL, getInternalName(m.getDeclaringClass()),
								m.getName(), getMethodDescriptor(m));
					}
				}

				returnType = m.getReturnType();

				stacksize++;
			} else {
				if ((m.getModifiers() & STATIC) == 0) {
					assert debug("CHECKCAST " + getInternalName(cls));
					mv.visitTypeInsn(CHECKCAST, getInternalName(cls));
				}

				for (int i = 0; i < es.length; i++) {
					if (es[i] instanceof ExecutableLiteral) {
						final ExecutableLiteral literal = (ExecutableLiteral) es[i];

						if (literal.getLiteral() == null) {
							assert debug("ICONST_NULL");
							mv.visitInsn(ACONST_NULL);
							continue;
						} else if (parameterTypes[i] == int.class && literal.intOptimized()) {
							intPush(literal.getInteger32());
							continue;
						} else if (parameterTypes[i] == int.class && preConvArgs[i] instanceof Integer) {
							intPush((Integer) preConvArgs[i]);
							continue;
						} else if (parameterTypes[i] == boolean.class) {
							final boolean bool = DataConversion.convert(literal.getLiteral(),
									Boolean.class);
							assert debug(bool ? "ICONST_1" : "ICONST_0");
							mv.visitInsn(bool ? ICONST_1 : ICONST_0);
							continue;
						} else {
							final Object lit = literal.getLiteral();

							if (parameterTypes[i] == Object.class) {
								if (isPrimitiveWrapper(lit.getClass())) {
									if (lit.getClass() == Integer.class) {
										intPush((Integer) lit);
									} else {
										assert debug("LDC " + lit);
										mv.visitLdcInsn(lit);
									}

									wrapPrimitive(lit.getClass());
								} else if (lit instanceof String) {
									mv.visitLdcInsn(lit);
									checkcast(Object.class);
								}
								continue;
							} else if (canConvert(parameterTypes[i], lit.getClass())) {
								final Object c = convert(lit, parameterTypes[i]);
								if (c instanceof Class) {
									ldcClassConstant((Class) c);
								} else {
									assert debug("LDC " + lit + " (" + lit.getClass().getName() + ")");

									mv.visitLdcInsn(convert(lit, parameterTypes[i]));

									if (isPrimitiveWrapper(parameterTypes[i])) {
										wrapPrimitive(lit.getClass());
									}
								}
								continue;
							} else {
								throw new OptimizationNotSupported();
							}
						}
					}

					assert debug("ALOAD 0");
					mv.visitVarInsn(ALOAD, 0);

					assert debug("GETFIELD p" + inputsOffset);
					mv.visitFieldInsn(GETFIELD, className, "p" + inputsOffset, "L" + NAMESPACE
							+ "compiler/ExecutableStatement;");

					inputsOffset++;

					assert debug("ALOAD 2");
					mv.visitVarInsn(ALOAD, 2);

					assert debug("ALOAD 3");
					mv.visitVarInsn(ALOAD, 3);

					assert debug("INVOKEINTERFACE ExecutableStatement.getValue");
					mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(ExecutableStatement.class),
							"getValue", "(Ljava/lang/Object;L" + NAMESPACE
									+ "integration/VariableResolverFactory;)Ljava/lang/Object;");

					if (parameterTypes[i].isPrimitive()) {
						if (preConvArgs[i] == null
								|| (parameterTypes[i] != String.class && !parameterTypes[i]
										.isAssignableFrom(preConvArgs[i].getClass()))) {

							ldcClassConstant(getWrapperClass(parameterTypes[i]));

							assert debug("INVOKESTATIC DataConversion.convert");
							mv.visitMethodInsn(INVOKESTATIC, NAMESPACE + "DataConversion", "convert",
									"(Ljava/lang/Object;Ljava/lang/Class;)Ljava/lang/Object;");
						}

						unwrapPrimitive(parameterTypes[i]);
					} else if (preConvArgs[i] == null
							|| (parameterTypes[i] != String.class && !parameterTypes[i]
									.isAssignableFrom(preConvArgs[i].getClass()))) {

						ldcClassConstant(parameterTypes[i]);

						assert debug("INVOKESTATIC DataConversion.convert");
						mv.visitMethodInsn(INVOKESTATIC, NAMESPACE + "DataConversion", "convert",
								"(Ljava/lang/Object;Ljava/lang/Class;)Ljava/lang/Object;");

						assert debug("CHECKCAST " + getInternalName(parameterTypes[i]));
						mv.visitTypeInsn(CHECKCAST, getInternalName(parameterTypes[i]));
					} else if (parameterTypes[i] == String.class) {
						assert debug("<<<DYNAMIC TYPE OPTIMIZATION STRING>>");
						mv.visitMethodInsn(INVOKESTATIC, "java/lang/String", "valueOf",
								"(Ljava/lang/Object;)Ljava/lang/String;");
					} else {
						assert debug("<<<DYNAMIC TYPING BYPASS>>>");
						assert debug("<<<OPT. JUSTIFICATION " + parameterTypes[i] + "="
								+ preConvArgs[i].getClass() + ">>>");

						assert debug("CHECKCAST " + getInternalName(parameterTypes[i]));
						mv.visitTypeInsn(CHECKCAST, getInternalName(parameterTypes[i]));
					}
				}

				if ((m.getModifiers() & STATIC) != 0) {
					assert debug("INVOKESTATIC: " + m.getName());
					mv.visitMethodInsn(INVOKESTATIC, getInternalName(m.getDeclaringClass()),
							m.getName(), getMethodDescriptor(m));
				} else {
					if (m.getDeclaringClass().isInterface()
							&& (m.getDeclaringClass() != cls || (ctx != null && ctx.getClass() != m
									.getDeclaringClass()))) {
						assert debug("INVOKEINTERFACE: " + getInternalName(m.getDeclaringClass()) + "."
								+ m.getName());
						mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(m.getDeclaringClass()),
								m.getName(), getMethodDescriptor(m));
					} else {
						assert debug("INVOKEVIRTUAL: " + getInternalName(cls) + "." + m.getName());
						mv.visitMethodInsn(INVOKEVIRTUAL, getInternalName(cls), m.getName(),
								getMethodDescriptor(m));
					}
				}

				returnType = m.getReturnType();

				stacksize++;
			}

			Object o = m.invoke(ctx, normalizeArgsForVarArgs(parameterTypes, args, m.isVarArgs()));

			if (hasNullMethodHandler()) {
				writeOutNullHandler(m, 1);
				if (o == null) {
					o = getNullMethodHandler().getProperty(m.getName(), ctx, variableFactory);
				}
			}

			currType = toNonPrimitiveType(m.getReturnType());
			return o;

		}
	}

	private void dataConversion(final Class target) {
		if (target.equals(Object.class)) {
			return;
		}

		ldcClassConstant(target);
		assert debug("INVOKESTATIC " + NAMESPACE + "DataConversion.convert");
		mv.visitMethodInsn(INVOKESTATIC, NAMESPACE + "DataConversion", "convert",
				"(Ljava/lang/Object;Ljava/lang/Class;)Ljava/lang/Object;");
	}

	private static MVELClassLoader classLoader;

	public static void setMVELClassLoader(final MVELClassLoader cl) {
		classLoader = cl;
	}

	public static MVELClassLoader getMVELClassLoader() {
		return classLoader;
	}

	@Override
	public void init() {
		try {
			classLoader = new JITClassLoader(currentThread().getContextClassLoader());
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	private ContextClassLoader getContextClassLoader() {
		return pCtx == null ? null : new ContextClassLoader(pCtx.getClassLoader());
	}

	private static class ContextClassLoader extends ClassLoader {
		ContextClassLoader(final ClassLoader classLoader) {
			super(classLoader);
		}

		Class<?> defineClass(final String name, final byte[] b) {
			return defineClass(name, b, 0, b.length);
		}
	}

	private java.lang.Class loadClass(final String className, final byte[] b) throws Exception {
		/**
		 * This must be synchronized. Two classes cannot be simultaneously
		 * deployed in the JVM.
		 */
		final ContextClassLoader contextClassLoader = getContextClassLoader();
		return contextClassLoader == null ? classLoader.defineClassX(className, b, 0, b.length)
				: contextClassLoader.defineClass(className, b);
	}

	private boolean debug(final String instruction) {
		if (buildLog != null) {
			buildLog.append(instruction).append("\n");
		}
		return true;
	}

	@SuppressWarnings({ "SameReturnValue" })
	public String getName() {
		return "ASM";
	}

	@Override
	public Object getResultOptPass() {
		return val;
	}

	private Class getWrapperClass(final Class cls) {
		if (cls == boolean.class) {
			return Boolean.class;
		} else if (cls == int.class) {
			return Integer.class;
		} else if (cls == float.class) {
			return Float.class;
		} else if (cls == double.class) {
			return Double.class;
		} else if (cls == short.class) {
			return Short.class;
		} else if (cls == long.class) {
			return Long.class;
		} else if (cls == byte.class) {
			return Byte.class;
		} else if (cls == char.class) {
			return Character.class;
		}

		return cls;
	}

	private void unwrapPrimitive(final Class cls) {
		if (cls == boolean.class) {
			assert debug("CHECKCAST java/lang/Boolean");
			mv.visitTypeInsn(CHECKCAST, "java/lang/Boolean");
			assert debug("INVOKEVIRTUAL java/lang/Boolean.booleanValue");
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z");
		} else if (cls == int.class) {
			assert debug("CHECKCAST java/lang/Integer");
			mv.visitTypeInsn(CHECKCAST, "java/lang/Integer");
			assert debug("INVOKEVIRTUAL java/lang/Integer.intValue");
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I");
		} else if (cls == float.class) {
			assert debug("CHECKCAST java/lang/Float");
			mv.visitTypeInsn(CHECKCAST, "java/lang/Float");
			assert debug("INVOKEVIRTUAL java/lang/Float.floatValue");
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F");
		} else if (cls == double.class) {
			assert debug("CHECKCAST java/lang/Double");
			mv.visitTypeInsn(CHECKCAST, "java/lang/Double");
			assert debug("INVOKEVIRTUAL java/lang/Double.doubleValue");
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D");
		} else if (cls == short.class) {
			assert debug("CHECKCAST java/lang/Short");
			mv.visitTypeInsn(CHECKCAST, "java/lang/Short");
			assert debug("INVOKEVIRTUAL java/lang/Short.shortValue");
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S");
		} else if (cls == long.class) {
			assert debug("CHECKCAST java/lang/Long");
			mv.visitTypeInsn(CHECKCAST, "java/lang/Long");
			assert debug("INVOKEVIRTUAL java/lang/Long.longValue");
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J");
		} else if (cls == byte.class) {
			assert debug("CHECKCAST java/lang/Byte");
			mv.visitTypeInsn(CHECKCAST, "java/lang/Byte");
			assert debug("INVOKEVIRTUAL java/lang/Byte.byteValue");
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B");
		} else if (cls == char.class) {
			assert debug("CHECKCAST java/lang/Character");
			mv.visitTypeInsn(CHECKCAST, "java/lang/Character");
			assert debug("INVOKEVIRTUAL java/lang/Character.charValue");
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C");
		}
	}

	private void wrapPrimitive(final Class<? extends Object> cls) {
		if (OPCODES_VERSION == Opcodes.V1_4) {
			/**
			 * JAVA 1.4 SUCKS! DIE 1.4 DIE!!!
			 */

			debug("** Using 1.4 Bytecode **");

			if (cls == boolean.class || cls == Boolean.class) {
				debug("NEW java/lang/Boolean");
				mv.visitTypeInsn(NEW, "java/lang/Boolean");

				debug("DUP X1");
				mv.visitInsn(DUP_X1);

				debug("SWAP");
				mv.visitInsn(SWAP);

				debug("INVOKESPECIAL java/lang/Boolean.<init>::(Z)V");
				mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Boolean", "<init>", "(Z)V");
			} else if (cls == int.class || cls == Integer.class) {
				debug("NEW java/lang/Integer");
				mv.visitTypeInsn(NEW, "java/lang/Integer");

				debug("DUP X1");
				mv.visitInsn(DUP_X1);

				debug("SWAP");
				mv.visitInsn(SWAP);

				debug("INVOKESPECIAL java/lang/Integer.<init>::(I)V");
				mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Integer", "<init>", "(I)V");
			} else if (cls == float.class || cls == Float.class) {
				debug("NEW java/lang/Float");
				mv.visitTypeInsn(NEW, "java/lang/Float");

				debug("DUP X1");
				mv.visitInsn(DUP_X1);

				debug("SWAP");
				mv.visitInsn(SWAP);

				debug("INVOKESPECIAL java/lang/Float.<init>::(F)V");
				mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Float", "<init>", "(F)V");
			} else if (cls == double.class || cls == Double.class) {
				debug("NEW java/lang/Double");
				mv.visitTypeInsn(NEW, "java/lang/Double");

				debug("DUP X2");
				mv.visitInsn(DUP_X2);

				debug("DUP X2");
				mv.visitInsn(DUP_X2);

				debug("POP");
				mv.visitInsn(POP);

				debug("INVOKESPECIAL java/lang/Double.<init>::(D)V");
				mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Double", "<init>", "(D)V");
			} else if (cls == short.class || cls == Short.class) {
				debug("NEW java/lang/Short");
				mv.visitTypeInsn(NEW, "java/lang/Short");

				debug("DUP X1");
				mv.visitInsn(DUP_X1);

				debug("SWAP");
				mv.visitInsn(SWAP);

				debug("INVOKESPECIAL java/lang/Short.<init>::(S)V");
				mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Short", "<init>", "(S)V");
			} else if (cls == long.class || cls == Long.class) {
				debug("NEW java/lang/Long");
				mv.visitTypeInsn(NEW, "java/lang/Long");

				debug("DUP X1");
				mv.visitInsn(DUP_X1);

				debug("SWAP");
				mv.visitInsn(SWAP);

				debug("INVOKESPECIAL java/lang/Long.<init>::(L)V");
				mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Float", "<init>", "(L)V");
			} else if (cls == byte.class || cls == Byte.class) {
				debug("NEW java/lang/Byte");
				mv.visitTypeInsn(NEW, "java/lang/Byte");

				debug("DUP X1");
				mv.visitInsn(DUP_X1);

				debug("SWAP");
				mv.visitInsn(SWAP);

				debug("INVOKESPECIAL java/lang/Byte.<init>::(B)V");
				mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Byte", "<init>", "(B)V");
			} else if (cls == char.class || cls == Character.class) {
				debug("NEW java/lang/Character");
				mv.visitTypeInsn(NEW, "java/lang/Character");

				debug("DUP X1");
				mv.visitInsn(DUP_X1);

				debug("SWAP");
				mv.visitInsn(SWAP);

				debug("INVOKESPECIAL java/lang/Character.<init>::(C)V");
				mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Character", "<init>", "(C)V");
			}
		} else {
			if (cls == boolean.class || cls == Boolean.class) {
				debug("INVOKESTATIC java/lang/Boolean.valueOf");
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf",
						"(Z)Ljava/lang/Boolean;");
			} else if (cls == int.class || cls == Integer.class) {
				debug("INVOKESTATIC java/lang/Integer.valueOf");
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf",
						"(I)Ljava/lang/Integer;");
			} else if (cls == float.class || cls == Float.class) {
				debug("INVOKESTATIC java/lang/Float.valueOf");
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;");
			} else if (cls == double.class || cls == Double.class) {
				debug("INVOKESTATIC java/lang/Double.valueOf");
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;");
			} else if (cls == short.class || cls == Short.class) {
				debug("INVOKESTATIC java/lang/Short.valueOf");
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;");
			} else if (cls == long.class || cls == Long.class) {
				debug("INVOKESTATIC java/lang/Long.valueOf");
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;");
			} else if (cls == byte.class || cls == Byte.class) {
				debug("INVOKESTATIC java/lang/Byte.valueOf");
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;");
			} else if (cls == char.class || cls == Character.class) {
				debug("INVOKESTATIC java/lang/Character.valueOf");
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Character", "valueOf",
						"(C)Ljava/lang/Character;");
			}
		}
	}

	private void anyArrayCheck(final Class cls) {
		if (cls == boolean[].class) {
			assert debug("CHECKCAST [Z");
			mv.visitTypeInsn(CHECKCAST, "[Z");
		} else if (cls == int[].class) {
			assert debug("CHECKCAST [I");
			mv.visitTypeInsn(CHECKCAST, "[I");
		} else if (cls == float[].class) {
			assert debug("CHECKCAST [F");
			mv.visitTypeInsn(CHECKCAST, "[F");
		} else if (cls == double[].class) {
			assert debug("CHECKCAST [D");
			mv.visitTypeInsn(CHECKCAST, "[D");
		} else if (cls == short[].class) {
			assert debug("CHECKCAST [S");
			mv.visitTypeInsn(CHECKCAST, "[S");
		} else if (cls == long[].class) {
			assert debug("CHECKCAST [J");
			mv.visitTypeInsn(CHECKCAST, "[J");
		} else if (cls == byte[].class) {
			assert debug("CHECKCAST [B");
			mv.visitTypeInsn(CHECKCAST, "[B");
		} else if (cls == char[].class) {
			assert debug("CHECKCAST [C");
			mv.visitTypeInsn(CHECKCAST, "[C");
		} else {
			assert debug("CHECKCAST [Ljava/lang/Object;");
			mv.visitTypeInsn(CHECKCAST, "[Ljava/lang/Object;");
		}
	}

	private void writeOutLiteralWrapped(final Object lit) {
		if (lit instanceof Integer) {
			intPush((Integer) lit);
			wrapPrimitive(int.class);
			return;
		}

		assert debug("LDC " + lit);
		if (lit instanceof String) {
			mv.visitLdcInsn(lit);
		} else if (lit instanceof Long) {
			mv.visitLdcInsn(lit);
			wrapPrimitive(long.class);
		} else if (lit instanceof Float) {
			mv.visitLdcInsn(lit);
			wrapPrimitive(float.class);
		} else if (lit instanceof Double) {
			mv.visitLdcInsn(lit);
			wrapPrimitive(double.class);
		} else if (lit instanceof Short) {
			mv.visitLdcInsn(lit);
			wrapPrimitive(short.class);
		} else if (lit instanceof Character) {
			mv.visitLdcInsn(lit);
			wrapPrimitive(char.class);
		} else if (lit instanceof Boolean) {
			mv.visitLdcInsn(lit);
			wrapPrimitive(boolean.class);
		} else if (lit instanceof Byte) {
			mv.visitLdcInsn(lit);
			wrapPrimitive(byte.class);
		}
	}

	public void arrayStore(final Class cls) {
		if (cls.isPrimitive()) {
			if (cls == int.class) {
				assert debug("IASTORE");
				mv.visitInsn(IASTORE);
			} else if (cls == char.class) {
				assert debug("CASTORE");
				mv.visitInsn(CASTORE);
			} else if (cls == boolean.class) {
				assert debug("BASTORE");
				mv.visitInsn(BASTORE);
			} else if (cls == double.class) {
				assert debug("DASTORE");
				mv.visitInsn(DASTORE);
			} else if (cls == float.class) {
				assert debug("FASTORE");
				mv.visitInsn(FASTORE);
			} else if (cls == short.class) {
				assert debug("SASTORE");
				mv.visitInsn(SASTORE);
			} else if (cls == long.class) {
				assert debug("LASTORE");
				mv.visitInsn(LASTORE);
			} else if (cls == byte.class) {
				assert debug("BASTORE");
				mv.visitInsn(BASTORE);
			}
		} else {
			assert debug("AASTORE");
			mv.visitInsn(AASTORE);
		}

	}

	public void wrapRuntimeConverstion(final Class toType) {
		ldcClassConstant(getWrapperClass(toType));

		assert debug("INVOKESTATIC DataConversion.convert");
		mv.visitMethodInsn(INVOKESTATIC, "" + NAMESPACE + "DataConversion", "convert",
				"(Ljava/lang/Object;Ljava/lang/Class;)Ljava/lang/Object;");
	}

	private Object addSubstatement(final ExecutableStatement stmt) {
		if (stmt instanceof ExecutableAccessor) {
			final ExecutableAccessor ea = (ExecutableAccessor) stmt;
			if (ea.getNode().isIdentifier() && !ea.getNode().isDeepProperty()) {
				loadVariableByName(ea.getNode().getName());
				return null;
			}
		}

		compiledInputs.add(stmt);

		assert debug("ALOAD 0");
		mv.visitVarInsn(ALOAD, 0);

		assert debug("GETFIELD p" + (compiledInputs.size() - 1));
		mv.visitFieldInsn(GETFIELD, className, "p" + (compiledInputs.size() - 1), "L" + NAMESPACE
				+ "compiler/ExecutableStatement;");

		assert debug("ALOAD 2");
		mv.visitVarInsn(ALOAD, 2);

		assert debug("ALOAD 3");
		mv.visitVarInsn(ALOAD, 3);

		assert debug("INVOKEINTERFACE ExecutableStatement.getValue");
		mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(ExecutableStatement.class), "getValue",
				"(Ljava/lang/Object;L" + NAMESPACE
						+ "integration/VariableResolverFactory;)Ljava/lang/Object;");

		return null;
	}

	private void loadVariableByName(final String name) {
		assert debug("ALOAD 3");
		mv.visitVarInsn(ALOAD, 3);

		assert debug("LDC \"" + name + "\"");
		mv.visitLdcInsn(name);

		assert debug("INVOKEINTERFACE " + NAMESPACE
				+ "integration/VariableResolverFactory.getVariableResolver");
		mv.visitMethodInsn(INVOKEINTERFACE, "" + NAMESPACE + "integration/VariableResolverFactory",
				"getVariableResolver", "(Ljava/lang/String;)L" + NAMESPACE
						+ "integration/VariableResolver;");

		assert debug("INVOKEINTERFACE " + NAMESPACE + "integration/VariableResolver.getValue");
		mv.visitMethodInsn(INVOKEINTERFACE, "" + NAMESPACE + "integration/VariableResolver",
				"getValue", "()Ljava/lang/Object;");

		returnType = Object.class;
	}

	private void loadVariableByIndex(final int pos) {
		assert debug("ALOAD 3");
		mv.visitVarInsn(ALOAD, 3);

		assert debug("PUSH IDX VAL =" + pos);
		intPush(pos);

		assert debug("INVOKEINTERFACE " + NAMESPACE
				+ "integration/VariableResolverFactory.getIndexedVariableResolver");
		mv.visitMethodInsn(INVOKEINTERFACE, "" + NAMESPACE + "integration/VariableResolverFactory",
				"getIndexedVariableResolver", "(I)L" + NAMESPACE + "integration/VariableResolver;");

		assert debug("INVOKEINTERFACE " + NAMESPACE + "integration/VariableResolver.getValue");
		mv.visitMethodInsn(INVOKEINTERFACE, "" + NAMESPACE + "integration/VariableResolver",
				"getValue", "()Ljava/lang/Object;");

		returnType = Object.class;
	}

	private void loadField(final int number) {
		assert debug("ALOAD 0");
		mv.visitVarInsn(ALOAD, 0);

		assert debug("GETFIELD p" + number);
		mv.visitFieldInsn(GETFIELD, className, "p" + number, "L" + NAMESPACE
				+ "compiler/ExecutableStatement;");
	}

	private void ldcClassConstant(final Class cls) {
		if (OPCODES_VERSION == Opcodes.V1_4) {
			assert debug("LDC \"" + cls.getName() + "\"");
			mv.visitLdcInsn(cls.getName());
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Class", "forName",
					"(Ljava/lang/String;)Ljava/lang/Class;");
			final Label l4 = new Label();
			mv.visitJumpInsn(GOTO, l4);
			mv.visitTypeInsn(NEW, "java/lang/NoClassDefFoundError");
			mv.visitInsn(DUP_X1);
			mv.visitInsn(SWAP);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Throwable", "getMessage",
					"()Ljava/lang/String;");
			mv.visitMethodInsn(INVOKESPECIAL, "java/lang/NoClassDefFoundError", "<init>",
					"(Ljava/lang/String;)V");
			mv.visitInsn(ATHROW);
			mv.visitLabel(l4);
		} else {
			assert debug("LDC " + getType(cls));
			mv.visitLdcInsn(getType(cls));
		}
	}

	private void buildInputs() {
		if (compiledInputs.size() == 0) {
			return;
		}

		assert debug("\n{SETTING UP MEMBERS...}\n");

		final StringAppender constSig = new StringAppender("(");
		final int size = compiledInputs.size();

		for (int i = 0; i < size; i++) {
			assert debug("ACC_PRIVATE p" + i);
			cw.visitField(ACC_PRIVATE, "p" + i, "L" + NAMESPACE + "compiler/ExecutableStatement;",
					null, null).visitEnd();

			constSig.append("L" + NAMESPACE + "compiler/ExecutableStatement;");
		}
		constSig.append(")V");

		assert debug("\n{CREATING INJECTION CONSTRUCTOR}\n");

		final MethodVisitor cv = cw
				.visitMethod(ACC_PUBLIC, "<init>", constSig.toString(), null, null);
		cv.visitCode();
		assert debug("ALOAD 0");
		cv.visitVarInsn(ALOAD, 0);
		assert debug("INVOKESPECIAL java/lang/Object.<init>");
		cv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");

		for (int i = 0; i < size; i++) {
			assert debug("ALOAD 0");
			cv.visitVarInsn(ALOAD, 0);
			assert debug("ALOAD " + (i + 1));
			cv.visitVarInsn(ALOAD, i + 1);
			assert debug("PUTFIELD p" + i);
			cv.visitFieldInsn(PUTFIELD, className, "p" + i, "L" + NAMESPACE
					+ "compiler/ExecutableStatement;");
		}

		assert debug("RETURN");
		cv.visitInsn(RETURN);
		cv.visitMaxs(0, 0);
		cv.visitEnd();

		assert debug("}");
	}

	private static final int ARRAY = 0;
	private static final int LIST = 1;
	private static final int MAP = 2;
	private static final int VAL = 3;

	private int _getAccessor(final Object o, Class type) {
		if (o instanceof List) {
			assert debug("NEW " + LIST_IMPL);
			mv.visitTypeInsn(NEW, LIST_IMPL);

			assert debug("DUP");
			mv.visitInsn(DUP);

			assert debug("DUP");
			mv.visitInsn(DUP);

			intPush(((List) o).size());
			assert debug("INVOKESPECIAL " + LIST_IMPL + ".<init>");
			mv.visitMethodInsn(INVOKESPECIAL, LIST_IMPL, "<init>", "(I)V");

			for (final Object item : (List) o) {
				if (_getAccessor(item, type) != VAL) {
					assert debug("POP");
					mv.visitInsn(POP);
				}

				assert debug("INVOKEINTERFACE java/util/List.add");
				mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z");

				assert debug("POP");
				mv.visitInsn(POP);

				assert debug("DUP");
				mv.visitInsn(DUP);
			}

			returnType = List.class;

			return LIST;
		} else if (o instanceof Map) {
			assert debug("NEW " + MAP_IMPL);
			mv.visitTypeInsn(NEW, MAP_IMPL);

			assert debug("DUP");
			mv.visitInsn(DUP);

			assert debug("DUP");
			mv.visitInsn(DUP);

			intPush(((Map) o).size());

			assert debug("INVOKESPECIAL " + MAP_IMPL + ".<init>");
			mv.visitMethodInsn(INVOKESPECIAL, MAP_IMPL, "<init>", "(I)V");

			for (final Object item : ((Map) o).keySet()) {
				mv.visitTypeInsn(CHECKCAST, "java/util/Map");

				if (_getAccessor(item, type) != VAL) {
					assert debug("POP");
					mv.visitInsn(POP);
				}
				if (_getAccessor(((Map) o).get(item), type) != VAL) {
					assert debug("POP");
					mv.visitInsn(POP);
				}

				assert debug("INVOKEINTERFACE java/util/Map.put");
				mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "put",
						"(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");

				assert debug("POP");
				mv.visitInsn(POP);

				assert debug("DUP");
				mv.visitInsn(DUP);
			}

			returnType = Map.class;

			return MAP;
		} else if (o instanceof Object[]) {
			final Accessor[] a = new Accessor[((Object[]) o).length];
			int i = 0;
			int dim = 0;

			if (type != null) {
				final String nm = type.getName();
				while (nm.charAt(dim) == '[') {
					dim++;
				}
			} else {
				type = Object[].class;
				dim = 1;
			}

			try {
				intPush(((Object[]) o).length);
				assert debug("ANEWARRAY " + getInternalName(getSubComponentType(type)) + " ("
						+ ((Object[]) o).length + ")");
				mv.visitTypeInsn(ANEWARRAY, getInternalName(getSubComponentType(type)));

				final Class cls = dim > 1 ? findClass(null, repeatChar('[', dim - 1) + "L"
						+ getBaseComponentType(type).getName() + ";", pCtx) : type;

				assert debug("DUP");
				mv.visitInsn(DUP);

				for (final Object item : (Object[]) o) {
					intPush(i);

					if (_getAccessor(item, cls) != VAL) {
						assert debug("POP");
						mv.visitInsn(POP);
					}

					assert debug("AASTORE (" + o.hashCode() + ")");
					mv.visitInsn(AASTORE);

					assert debug("DUP");
					mv.visitInsn(DUP);

					i++;
				}

			} catch (final ClassNotFoundException e) {
				throw new RuntimeException("this error should never throw:"
						+ getBaseComponentType(type).getName(), e);
			}

			return ARRAY;
		} else {
			if (type.isArray()) {
				writeLiteralOrSubexpression(subCompileExpression(((String) o).toCharArray(), pCtx),
						getSubComponentType(type));
			} else {
				writeLiteralOrSubexpression(subCompileExpression(((String) o).toCharArray(), pCtx));
			}
			return VAL;
		}
	}

	private Class writeLiteralOrSubexpression(final Object stmt) {
		return writeLiteralOrSubexpression(stmt, null, null);
	}

	private Class writeLiteralOrSubexpression(final Object stmt, final Class desiredTarget) {
		return writeLiteralOrSubexpression(stmt, desiredTarget, null);
	}

	private Class writeLiteralOrSubexpression(final Object stmt, final Class desiredTarget,
			final Class knownIngressType) {
		if (stmt instanceof ExecutableLiteral) {
			Class type = ((ExecutableLiteral) stmt).getLiteral().getClass();

			assert debug("*** type:" + type + ";desired:" + desiredTarget);

			if (type == Integer.class && desiredTarget == int.class) {
				intPush(((ExecutableLiteral) stmt).getInteger32());
				type = int.class;
			} else if (desiredTarget != null && desiredTarget != type) {
				assert debug("*** Converting because desiredType(" + desiredTarget.getClass()
						+ ") is not: " + type);

				if (!DataConversion.canConvert(type, desiredTarget)) {
					throw new CompileException("was expecting type: " + desiredTarget.getName()
							+ "; but found type: " + type.getName(), expr, st);
				}
				writeOutLiteralWrapped(convert(((ExecutableLiteral) stmt).getLiteral(), desiredTarget));
			} else {
				writeOutLiteralWrapped(((ExecutableLiteral) stmt).getLiteral());
			}

			return type;
		} else {
			literal = false;

			addSubstatement((ExecutableStatement) stmt);

			Class type;
			if (knownIngressType == null) {
				type = ((ExecutableStatement) stmt).getKnownEgressType();
			} else {
				type = knownIngressType;
			}

			if (desiredTarget != null && type != desiredTarget) {
				if (desiredTarget.isPrimitive()) {
					if (type == null) {
						throw new OptimizationFailure("cannot optimize expression: " + new String(expr)
								+ ": cannot determine ingress type for primitive output");
					}

					checkcast(type);
					unwrapPrimitive(desiredTarget);
				}
			}

			return type;
		}
	}

	private void addPrintOut(final String text) {
		mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
		mv.visitLdcInsn(text);
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
	}

	@Override
	public Accessor optimizeCollection(final ParserContext pCtx, final Object o, Class type,
			final char[] property, final int start, final int offset, final Object ctx,
			final Object thisRef, final VariableResolverFactory factory) {
		this.expr = property;
		this.cursor = this.start = start;
		this.end = start + offset;
		this.length = offset;

		type = toNonPrimitiveArray(type);
		this.returnType = type;

		this.compiledInputs = new ArrayList<ExecutableStatement>();

		this.ctx = ctx;
		this.thisRef = thisRef;
		this.variableFactory = factory;

		_initJIT();

		literal = true;

		_getAccessor(o, type);

		_finishJIT();

		try {
			final Accessor compiledAccessor = _initializeAccessor();

			if (property != null && length > start) {
				return new Union(compiledAccessor, property, start, length);
			} else {
				return compiledAccessor;
			}

		} catch (final Exception e) {
			throw new OptimizationFailure("could not optimize collection", e);
		}
	}

	private void checkcast(final Class cls) {
		assert debug("CHECKCAST " + getInternalName(cls));
		mv.visitTypeInsn(CHECKCAST, getInternalName(cls));
	}

	private void intPush(final int index) {
		if (index < 6) {
			switch (index) {
			case 0:
				assert debug("ICONST_0");
				mv.visitInsn(ICONST_0);
				break;
			case 1:
				assert debug("ICONST_1");
				mv.visitInsn(ICONST_1);
				break;
			case 2:
				assert debug("ICONST_2");
				mv.visitInsn(ICONST_2);
				break;
			case 3:
				assert debug("ICONST_3");
				mv.visitInsn(ICONST_3);
				break;
			case 4:
				assert debug("ICONST_4");
				mv.visitInsn(ICONST_4);
				break;
			case 5:
				assert debug("ICONST_5");
				mv.visitInsn(ICONST_5);
				break;
			}
		} else if (index > -127 && index < 128) {
			assert debug("BIPUSH " + index);
			mv.visitIntInsn(BIPUSH, index);
		} else if (index > Short.MAX_VALUE) {
			assert debug("LDC " + index);
			mv.visitLdcInsn(index);
		} else {
			assert debug("SIPUSH " + index);
			mv.visitIntInsn(SIPUSH, index);
		}
	}

	@Override
	public Accessor optimizeObjectCreation(final ParserContext pCtx, final char[] property,
			final int start, final int offset, final Object ctx, final Object thisRef,
			final VariableResolverFactory factory) {
		_initJIT();

		compiledInputs = new ArrayList<ExecutableStatement>();
		this.start = cursor = start;
		this.end = start + offset;
		this.length = this.end - this.start;
		this.ctx = ctx;
		this.thisRef = thisRef;
		this.variableFactory = factory;
		this.pCtx = pCtx;

		final String[] cnsRes = captureContructorAndResidual(property, start, offset);
		final List<char[]> constructorParms = parseMethodOrConstructor(cnsRes[0].toCharArray());

		try {
			if (constructorParms != null) {
				for (final char[] constructorParm : constructorParms) {
					compiledInputs
							.add((ExecutableStatement) subCompileExpression(constructorParm, pCtx));
				}

				final Class cls = findClass(factory,
						new String(subset(property, 0, findFirst('(', start, length, property))), pCtx);

				assert debug("NEW " + getInternalName(cls));
				mv.visitTypeInsn(NEW, getInternalName(cls));
				assert debug("DUP");
				mv.visitInsn(DUP);

				final Object[] parms = new Object[constructorParms.size()];

				int i = 0;
				for (final ExecutableStatement es : compiledInputs) {
					parms[i++] = es.getValue(ctx, factory);
				}

				final Constructor cns = getBestConstructorCandidate(parms, cls, pCtx.isStrongTyping());

				if (cns == null) {
					final StringBuilder error = new StringBuilder();
					for (int x = 0; x < parms.length; x++) {
						error.append(parms[x].getClass().getName());
						if (x + 1 < parms.length) {
							error.append(", ");
						}
					}

					throw new CompileException("unable to find constructor: " + cls.getName() + "("
							+ error.toString() + ")", expr, st);
				}

				this.returnType = cns.getDeclaringClass();

				Class tg;
				for (i = 0; i < constructorParms.size(); i++) {
					assert debug("ALOAD 0");
					mv.visitVarInsn(ALOAD, 0);
					assert debug("GETFIELD p" + i);
					mv.visitFieldInsn(GETFIELD, className, "p" + i, "L" + NAMESPACE
							+ "compiler/ExecutableStatement;");
					assert debug("ALOAD 2");
					mv.visitVarInsn(ALOAD, 2);
					assert debug("ALOAD 3");
					mv.visitVarInsn(ALOAD, 3);
					assert debug("INVOKEINTERFACE " + NAMESPACE
							+ "compiler/ExecutableStatement.getValue");
					mv.visitMethodInsn(INVOKEINTERFACE, "" + NAMESPACE + "compiler/ExecutableStatement",
							"getValue", "(Ljava/lang/Object;L" + NAMESPACE
									+ "integration/VariableResolverFactory;)Ljava/lang/Object;");

					tg = cns.getParameterTypes()[i].isPrimitive() ? getWrapperClass(cns
							.getParameterTypes()[i]) : cns.getParameterTypes()[i];

					if (parms[i] != null
							&& !parms[i].getClass().isAssignableFrom(cns.getParameterTypes()[i])) {
						ldcClassConstant(tg);

						assert debug("INVOKESTATIC " + NAMESPACE + "DataConversion.convert");
						mv.visitMethodInsn(INVOKESTATIC, "" + NAMESPACE + "DataConversion", "convert",
								"(Ljava/lang/Object;Ljava/lang/Class;)Ljava/lang/Object;");

						if (cns.getParameterTypes()[i].isPrimitive()) {
							unwrapPrimitive(cns.getParameterTypes()[i]);
						} else {
							assert debug("CHECKCAST " + getInternalName(tg));
							mv.visitTypeInsn(CHECKCAST, getInternalName(tg));
						}

					} else {
						assert debug("CHECKCAST " + getInternalName(cns.getParameterTypes()[i]));
						mv.visitTypeInsn(CHECKCAST, getInternalName(cns.getParameterTypes()[i]));
					}

				}

				assert debug("INVOKESPECIAL " + getInternalName(cls) + ".<init> : "
						+ getConstructorDescriptor(cns));
				mv.visitMethodInsn(INVOKESPECIAL, getInternalName(cls), "<init>",
						getConstructorDescriptor(cns));

				_finishJIT();

				final Accessor acc = _initializeAccessor();

				if (cnsRes.length > 1 && cnsRes[1] != null && !cnsRes[1].trim().equals("")) {
					return new Union(acc, cnsRes[1].toCharArray(), 0, cnsRes[1].length());
				}

				return acc;
			} else {
				final Class cls = findClass(factory, new String(property), pCtx);

				assert debug("NEW " + getInternalName(cls));
				mv.visitTypeInsn(NEW, getInternalName(cls));
				assert debug("DUP");
				mv.visitInsn(DUP);

				final Constructor cns = cls.getConstructor(EMPTYCLS);

				assert debug("INVOKESPECIAL <init>");

				mv.visitMethodInsn(INVOKESPECIAL, getInternalName(cls), "<init>",
						getConstructorDescriptor(cns));

				_finishJIT();
				final Accessor acc = _initializeAccessor();

				if (cnsRes.length > 1 && cnsRes[1] != null && !cnsRes[1].trim().equals("")) {
					return new Union(acc, cnsRes[1].toCharArray(), 0, cnsRes[1].length());
				}

				return acc;
			}
		} catch (final ClassNotFoundException e) {
			throw new CompileException("class or class reference not found: " + new String(property),
					property, st);
		} catch (final Exception e) {
			throw new OptimizationFailure("could not optimize construtor: " + new String(property), e);
		}
	}

	@Override
	public Class getEgressType() {
		return returnType;
	}

	private void dumpAdvancedDebugging() {
		if (buildLog == null) {
			return;
		}

		System.out.println("JIT Compiler Dump for: <<" + (expr == null ? null : new String(expr))
				+ ">>\n-------------------------------\n");
		System.out.println(buildLog.toString());
		System.out.println("\n<END OF DUMP>\n");
		if (MVEL.isFileDebugging()) {
			try {
				final FileWriter writer = ParseTools.getDebugFileWriter();
				writer.write(buildLog.toString());
				writer.flush();
				writer.close();
			} catch (final IOException e) {
				// empty
			}
		}
	}

	private Object propHandlerByteCode(final String property, final Object ctx, final Class handler) {
		final PropertyHandler ph = getPropertyHandler(handler);
		if (ph instanceof ProducesBytecode) {
			assert debug("<<3rd-Party Code Generation>>");
			((ProducesBytecode) ph).produceBytecodeGet(mv, property, variableFactory);
			return ph.getProperty(property, ctx, variableFactory);
		} else {
			throw new RuntimeException(
					"unable to compileShared: custom accessor does not support producing bytecode: "
							+ ph.getClass().getName());
		}
	}

	private void propHandlerByteCodePut(final String property, final Object ctx,
			final Class handler, final Object value) {
		final PropertyHandler ph = getPropertyHandler(handler);
		if (ph instanceof ProducesBytecode) {
			assert debug("<<3rd-Party Code Generation>>");
			((ProducesBytecode) ph).produceBytecodePut(mv, property, variableFactory);
			ph.setProperty(property, ctx, variableFactory, value);
		} else {
			throw new RuntimeException(
					"unable to compileShared: custom accessor does not support producing bytecode: "
							+ ph.getClass().getName());
		}
	}

	private void writeOutNullHandler(final Member member, final int type) {

		assert debug("DUP");
		mv.visitInsn(DUP);

		final Label j = new Label();

		assert debug("IFNONNULL : jump");
		mv.visitJumpInsn(IFNONNULL, j);

		assert debug("POP");
		mv.visitInsn(POP);

		assert debug("ALOAD 0");
		mv.visitVarInsn(ALOAD, 0);

		if (type == 0) {
			this.propNull = true;

			assert debug("GETFIELD 'nullPropertyHandler'");
			mv.visitFieldInsn(GETFIELD, className, "nullPropertyHandler", "L" + NAMESPACE
					+ "integration/PropertyHandler;");
		} else {
			this.methNull = true;

			assert debug("GETFIELD 'nullMethodHandler'");
			mv.visitFieldInsn(GETFIELD, className, "nullMethodHandler", "L" + NAMESPACE
					+ "integration/PropertyHandler;");
		}

		assert debug("LDC '" + member.getName() + "'");
		mv.visitLdcInsn(member.getName());

		assert debug("ALOAD 1");
		mv.visitVarInsn(ALOAD, 1);

		assert debug("ALOAD 3");
		mv.visitVarInsn(ALOAD, 3);

		assert debug("INVOKEINTERFACE PropertyHandler.getProperty");
		mv.visitMethodInsn(INVOKEINTERFACE, NAMESPACE + "integration/PropertyHandler", "getProperty",
				"(Ljava/lang/String;Ljava/lang/Object;L" + NAMESPACE
						+ "integration/VariableResolverFactory;)Ljava/lang/Object;");

		assert debug("LABEL:jump");
		mv.visitLabel(j);

	}

	@Override
	public boolean isLiteralOnly() {
		return literal;
	}
}
