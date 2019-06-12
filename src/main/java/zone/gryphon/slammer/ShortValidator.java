package zone.gryphon.slammer;

import com.beust.jcommander.IValueValidator;
import com.beust.jcommander.ParameterException;

public class ShortValidator implements IValueValidator<Integer> {

    @Override
    public void validate(String name, Integer value) throws ParameterException {
        if (value > Short.MAX_VALUE) {
            throw new ParameterException(String.format("Parameter %s cannot be larger than %d", name, Short.MAX_VALUE));
        }
    }
}
