package com.badminton.core;

import com.badminton.entity.RentByTime;
import com.badminton.model.dto.RentByTimeDTO;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Component;

import java.beans.PropertyDescriptor;
import java.math.BigDecimal;
import java.util.Set;

@Component
public class CoreBeanUtils {
    /**
     * The row is addressed through the argument object, never overwritten by it.
     */
    private static final Set<String> NON_UPDATABLE = Set.of("id", "rentId", "class");

    /**
     * Properties whose argument-object type the column cannot hold (the state arrives as an
     * enum but is stored as text), so the owning core service writes them itself.
     */
    private static final Set<String> APPLIED_BY_CORE = Set.of("state");

    /**
     * Set the properties of the column object that are not null in the argument object.
     * The column object is modified in place; the argument object is not.
     *
     * @param rentByTimeDTO
     * @param rental
     */
    public void setRentByTimePropertiesIfNotNull(RentByTimeDTO rentByTimeDTO, RentByTime rental) {

        BeanWrapper carried = new BeanWrapperImpl(rentByTimeDTO);
        BeanWrapper column = new BeanWrapperImpl(rental);

        for (PropertyDescriptor property : column.getPropertyDescriptors()) {
            String name = property.getName();
            if (property.getWriteMethod() == null || NON_UPDATABLE.contains(name)
                    || APPLIED_BY_CORE.contains(name) || !carried.isReadableProperty(name)) {
                continue;
            }
            Object value = carried.getPropertyValue(name);
            if (!isProvided(value, carried.getPropertyType(name))) {
                continue;
            }
            column.setPropertyValue(name, adaptToColumn(value, property.getPropertyType()));
        }
    }

    /**
     * A primitive number in the argument object cannot express "not sent", so zero means
     * the caller left that property out; every other property uses null for that.
     */
    private boolean isProvided(Object value, Class<?> argumentType) {
        if (value == null) {
            return false;
        }
        if (argumentType == float.class || argumentType == double.class) {
            return ((Number) value).doubleValue() > 0;
        }
        if (argumentType == int.class || argumentType == long.class) {
            return ((Number) value).longValue() > 0;
        }
        return true;
    }

    /**
     * Bridge the one shape difference between an argument object and a column: durations
     * arrive as a primitive number but are stored as a decimal. Anything else is handed to
     * the setter as is, and a mismatch between the two classes fails loudly here.
     */
    private Object adaptToColumn(Object value, Class<?> columnType) {
        if (!columnType.isInstance(value) && columnType == BigDecimal.class && value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return value;
    }
}
