package org.openmrs.module.migrate.metadata;

import org.openmrs.module.metadatadeploy.bundle.AbstractMetadataBundle;
import org.springframework.stereotype.Component;

import static org.openmrs.module.metadatadeploy.bundle.CoreConstructors.encounterType;
import static org.openmrs.module.metadatadeploy.bundle.CoreConstructors.form;

/**
 * Created by derric on 6/19/14.
 */
@Component
public class MigrateMetadata extends AbstractMetadataBundle {

    public static class _EncounterType {
        public static final String migratedata = "71d53282-f7a9-11e3-bb18-28924a18f0d4";

    }

    public static class _Form {
        public static final String migratedata = "71d53282-f7a9-11e3-bb18-28924a18f0d4";

    }

    @Override
    public void install() {
        install(encounterType("migrate data", "migrate data", _EncounterType.migratedata));

        install(form("migrate data", null, _EncounterType.migratedata, "1", _Form.migratedata));
    }

}
