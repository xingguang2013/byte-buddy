package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.attribute.AnnotationValueFilter;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

/**
 * A type initializer is responsible for defining a type's static initialization block.
 */
public interface TypeInitializer extends ByteCodeAppender {

    /**
     * Indicates if this type initializer is defined.
     *
     * @return {@code true} if this type initializer is defined.
     */
    boolean isDefined();

    /**
     * Expands this type initializer with another byte code appender. For this to be possible, this type initializer must
     * be defined.
     *
     * @param byteCodeAppender The byte code appender to apply as the type initializer.
     * @return A defined type initializer.
     */
    TypeInitializer expandWith(ByteCodeAppender byteCodeAppender);

    /**
     * Creates a method pool record that applies this type initializer while preserving the record that was supplied.
     *
     * @param record The record to wrap.
     * @return A new record that represents the supplied record while also executing this type initializer.
     */
    TypeWriter.MethodPool.Record wrap(TypeWriter.MethodPool.Record record);

    /**
     * A drain for writing a type initializer.
     */
    interface Drain {

        /**
         * Applies the drain.
         *
         * @param classVisitor          The class visitor to apply the initializer to.
         * @param typeInitializer       The type initializer to write.
         * @param implementationContext The corresponding implementation context.
         */
        void apply(ClassVisitor classVisitor, TypeInitializer typeInitializer, Implementation.Context implementationContext);

        /**
         * A default implementation of a type initializer drain that creates a initializer method.
         */
        class Default implements Drain {

            /**
             * The instrumented type.
             */
            protected final TypeDescription instrumentedType;

            /**
             * The method pool to use.
             */
            protected final TypeWriter.MethodPool methodPool;

            /**
             * The annotation value filter factory to use.
             */
            protected final AnnotationValueFilter.Factory annotationValueFilterFactory;

            /**
             * Creates a new default type initializer drain.
             *
             * @param instrumentedType             The instrumented type.
             * @param methodPool                   The method pool to use.
             * @param annotationValueFilterFactory The annotation value filter factory to use.
             */
            public Default(TypeDescription instrumentedType,
                           TypeWriter.MethodPool methodPool,
                           AnnotationValueFilter.Factory annotationValueFilterFactory) {
                this.instrumentedType = instrumentedType;
                this.methodPool = methodPool;
                this.annotationValueFilterFactory = annotationValueFilterFactory;
            }

            @Override
            public void apply(ClassVisitor classVisitor, TypeInitializer typeInitializer, Implementation.Context implementationContext) {
                typeInitializer.wrap(methodPool.target(new MethodDescription.Latent.TypeInitializer(instrumentedType))).apply(classVisitor,
                        implementationContext,
                        annotationValueFilterFactory);
            }

            @Override
            public boolean equals(Object object) {
                if (this == object) return true;
                if (object == null || getClass() != object.getClass()) return false;
                Default aDefault = (Default) object;
                return instrumentedType.equals(aDefault.instrumentedType)
                        && methodPool.equals(aDefault.methodPool)
                        && annotationValueFilterFactory.equals(aDefault.annotationValueFilterFactory);
            }

            @Override
            public int hashCode() {
                int result = instrumentedType.hashCode();
                result = 31 * result + methodPool.hashCode();
                result = 31 * result + annotationValueFilterFactory.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "TypeInitializer.Drain.Default{" +
                        "instrumentedType=" + instrumentedType +
                        ", methodPool=" + methodPool +
                        ", annotationValueFilterFactory=" + annotationValueFilterFactory +
                        '}';
            }
        }
    }

    /**
     * Canonical implementation of a non-defined type initializer.
     */
    enum None implements TypeInitializer {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public boolean isDefined() {
            return false;
        }

        @Override
        public TypeInitializer expandWith(ByteCodeAppender byteCodeAppenderFactory) {
            return new TypeInitializer.Simple(byteCodeAppenderFactory);
        }

        @Override
        public TypeWriter.MethodPool.Record wrap(TypeWriter.MethodPool.Record record) {
            return record;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext, MethodDescription instrumentedMethod) {
            return new Size(0, 0);
        }

        @Override
        public String toString() {
            return "TypeInitializer.None." + name();
        }
    }

    /**
     * A simple, defined type initializer that executes a given {@link ByteCodeAppender}.
     */
    class Simple implements TypeInitializer {

        /**
         * The byte code appender to apply as the type initializer.
         */
        private final ByteCodeAppender byteCodeAppender;

        /**
         * Creates a new simple type initializer.
         *
         * @param byteCodeAppender The byte code appender to apply as the type initializer.
         */
        public Simple(ByteCodeAppender byteCodeAppender) {
            this.byteCodeAppender = byteCodeAppender;
        }

        @Override
        public boolean isDefined() {
            return true;
        }

        @Override
        public TypeInitializer expandWith(ByteCodeAppender byteCodeAppender) {
            return new TypeInitializer.Simple(new Compound(this.byteCodeAppender, byteCodeAppender));
        }

        @Override
        public TypeWriter.MethodPool.Record wrap(TypeWriter.MethodPool.Record record) {
            return record.prepend(byteCodeAppender);
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext, MethodDescription instrumentedMethod) {
            return byteCodeAppender.apply(methodVisitor, implementationContext, instrumentedMethod);
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && byteCodeAppender.equals(((TypeInitializer.Simple) other).byteCodeAppender);
        }

        @Override
        public int hashCode() {
            return byteCodeAppender.hashCode();
        }

        @Override
        public String toString() {
            return "TypeInitializer.Simple{" +
                    "byteCodeAppender=" + byteCodeAppender +
                    '}';
        }
    }
}
