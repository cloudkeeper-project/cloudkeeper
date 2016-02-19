@XmlSchema(
    namespace = JAXBConstants.NAMESPACE,
    location = JAXBConstants.LOCATION,
    elementFormDefault = XmlNsForm.QUALIFIED
)
@XmlJavaTypeAdapters(
    @XmlJavaTypeAdapter(value = JAXBAdapters.PatternAdapter.class, type = Pattern.class)
)
@NonNullByDefault
package xyz.cloudkeeper.model.beans.execution;

import xyz.cloudkeeper.model.util.JAXBConstants;
import xyz.cloudkeeper.model.util.NonNullByDefault;

import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapters;
import java.util.regex.Pattern;
