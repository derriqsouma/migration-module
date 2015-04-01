package org.openmrs.module.migrate;

import org.openmrs.module.metadatadeploy.bundle.AbstractMetadataBundle;
import org.springframework.stereotype.Component;

import static org.openmrs.module.metadatadeploy.bundle.CoreConstructors.globalProperty;

/**
 * Created by derric on 4/1/15.
 */
@Component
public class FilePath extends AbstractMetadataBundle {
    @Override
    public void install() throws Exception {
        install(globalProperty(MigrateConstant.FILE_PATH, "The path where the excel files are located", null));
    }
}
