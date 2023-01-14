package org.copalis.builder;
import java.io.IOException;
import java.util.Set;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;

public class ClassFileManager {

    public static JavaFileManager wrap(JavaFileManager fm, Set<JavaFileObject> classFiles) {
        return new ForwardingJavaFileManager<JavaFileManager>(fm) {

            @Override public JavaFileObject getJavaFileForOutput(
                    Location location, String className, Kind kind, FileObject sibling) throws IOException {

                JavaFileObject file = super.getJavaFileForOutput(location, className, kind, sibling);
                if (file.getKind() == JavaFileObject.Kind.CLASS) {
                    classFiles.add(file);
                }
                return file;
            }
        };
    }

//    public static <X> X instrument(X core, Class<X> type) {
//        return type.cast(
//                Proxy.newProxyInstance(
//                        type.getClassLoader(),
//                        new Class<?>[] {type},
//                        (proxy, method, args) -> {
//                            System.err.println(method.getName() + (Objects.nonNull(args) ? Arrays.asList(args) : ""));
//                            if (method.isDefault()) {
//                                return InvocationHandler.invokeDefault(proxy, method, args);
//                            } else {
//                                return method.invoke(core, args);
//                            }
//                        }
//        ));
//    }
}