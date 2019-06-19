package zone.gryphon.jordan.input;

import com.beust.jcommander.IStringConverter;

import java.io.File;

/**
 * @author galen
 */


public class RequiredFileConverter implements IStringConverter<File> {

    @Override
    public File convert(String value) {
        File file = new File(value);

        if (!file.exists()) {
            throw new IllegalArgumentException("File \"" + value + "\" does not exist!");
        }

        return file;
    }
}


