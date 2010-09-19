package javax.tools;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.net.URI;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;

/**
 * This class is for subclass
 * 
 */
public class SimpleJavaFileObject implements FileObject, JavaFileObject {
    protected final Kind kind;

    protected final URI uri;

    protected SimpleJavaFileObject(URI uri, JavaFileObject.Kind kind) {
        this.uri = uri;
        this.kind = kind;
    }

    public boolean delete() {
        // do nothing
        return false;
    }

    public Modifier getAccessLevel() {
        // do nothing
        return null;
    }

    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        // do nothing here
        throw new UnsupportedOperationException();
    }

    public JavaFileObject.Kind getKind() {
        return kind;
    }

    public long getLastModified() {
        return 0L;
    }

    public String getName() {
        return uri.getClass().toString();
    }

    public NestingKind getNestingKind() {
        return null;
    }

    public boolean isNameCompatible(String simpleName, JavaFileObject.Kind kind) {
        if (this.kind.equals(kind)) {
            String path = uri.getPath();
            String givenPath = simpleName + kind.extension;
            if (path.equals(givenPath) || path.endsWith("/" + givenPath)) {
                return true;
            }
        }
        return false;
    }

    public InputStream openInputStream() {
        throw new UnsupportedOperationException();
    }

    public OutputStream openOutputStream() {
        throw new UnsupportedOperationException();
    }

    public Reader openReader(boolean ignoreEncodingErrors) {
        return new StringReader(getCharContent(ignoreEncodingErrors).toString());
    }

    public Writer openWriter() {
        return new OutputStreamWriter(openOutputStream());
    }

    public String toString() {
        return getClass().getName() + '@' + Integer.toHexString(hashCode());
    }

    public URI toUri() {
        return uri;
    }
}
