/**
 * Defines immutable domain-specific wrappers around primitive data types.
 *
 * The classes in this package have in common that they can be instantiated from a primitive Java type, a String, or
 * an array of the one of the aforementioned types.
 */
@XmlSchema(
    namespace = JAXBConstants.NAMESPACE,
    location = JAXBConstants.LOCATION,
    elementFormDefault = XmlNsForm.QUALIFIED
)
@NonNullByDefault
package xyz.cloudkeeper.model.immutable;

import xyz.cloudkeeper.model.util.JAXBConstants;
import xyz.cloudkeeper.model.util.NonNullByDefault;

import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;
