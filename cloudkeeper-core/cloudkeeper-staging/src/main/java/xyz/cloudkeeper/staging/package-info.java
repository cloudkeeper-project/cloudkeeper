/**
 * Provides a ready-to-use in-memory staging-area implementation and a skeletal external-storage staging-area
 * implementation.
 */
@XmlSchema(
    namespace = "http://www.svbio.com/cloudkeeper/staging/2.0.0",
    elementFormDefault = XmlNsForm.QUALIFIED
)
@NonNullByDefault
package xyz.cloudkeeper.staging;

import xyz.cloudkeeper.model.util.NonNullByDefault;

import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;
