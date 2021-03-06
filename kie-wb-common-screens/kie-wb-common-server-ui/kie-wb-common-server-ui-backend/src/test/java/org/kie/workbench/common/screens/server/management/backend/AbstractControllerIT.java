/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.workbench.common.screens.server.management.backend;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.strategy.CombinedStrategy;
import org.jboss.shrinkwrap.resolver.api.maven.strategy.RejectDependenciesStrategy;
import org.jboss.shrinkwrap.resolver.api.maven.strategy.TransitiveStrategy;
import org.kie.server.controller.api.model.spec.ServerTemplateList;
import org.kie.server.controller.impl.KieServerHealthCheckControllerImpl;
import org.kie.workbench.common.screens.server.management.backend.rest.StandaloneControllerDynamicFeature;
import org.kie.workbench.common.screens.server.management.backend.rest.StandaloneControllerFilter;
import org.kie.workbench.common.screens.server.management.backend.runtime.AsyncKieServerInstanceManager;
import org.kie.workbench.common.screens.server.management.backend.service.EmbeddedNotificationService;
import org.kie.workbench.common.screens.server.management.backend.service.RuleCapabilitiesServiceCDI;
import org.kie.workbench.common.screens.server.management.backend.service.RuntimeManagementServiceCDI;
import org.kie.workbench.common.screens.server.management.backend.service.SpecManagementServiceCDI;
import org.kie.workbench.common.screens.server.management.backend.storage.ServerTemplateOCPStorage;
import org.kie.workbench.common.screens.server.management.backend.storage.ServerTemplateVFSStorage;
import org.kie.workbench.common.screens.server.management.backend.storage.migration.ServerTemplateMigration;
import org.kie.workbench.common.screens.server.management.backend.utils.AppSetup;
import org.kie.workbench.common.screens.server.management.backend.utils.ControllerExtension;
import org.kie.workbench.common.screens.server.management.backend.utils.EmbeddedController;
import org.kie.workbench.common.screens.server.management.backend.utils.MVELEvaluatorProducer;
import org.kie.workbench.common.screens.server.management.backend.utils.MockTestService;
import org.kie.workbench.common.screens.server.management.backend.utils.StandaloneController;
import org.kie.workbench.common.screens.server.management.backend.websocket.StandaloneControllerApplicationConfig;
import org.kie.workbench.common.screens.server.management.backend.websocket.StandaloneNotificationService;
import org.kie.workbench.common.screens.server.management.utils.ControllerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public abstract class AbstractControllerIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractControllerIT.class);

    protected static final String USER = "admin";
    protected static final String PASSWORD = "admin";
    protected static final String SERVER_TEMPLATE_ID = "it-test-kie-server";
    
    private static File[] getLibraries(){
        final File[] libraries = Maven.configureResolver().workOffline().loadPomFromFile("pom.xml")
                .importRuntimeDependencies()
                .resolve(
                        "org.kie.workbench.screens:kie-wb-common-workbench-backend",
                        "org.kie.workbench.services:kie-wb-common-services-backend",
                        "org.uberfire:uberfire-nio2-fs",
                        "org.uberfire:uberfire-nio2-jgit",
                        "org.uberfire:uberfire-ssh-api",
                        "org.uberfire:uberfire-ssh-backend",
                        "org.uberfire:uberfire-servlet-security",
                        "org.uberfire:uberfire-rest-backend",
                        "org.uberfire:uberfire-backend-server",
                        "org.uberfire:uberfire-backend-cdi",
                        "org.jboss.errai:errai-jboss-as-support",
                        "org.jboss.errai:errai-security-server",
                        "org.jboss.errai:errai-cdi-server",
                        "org.mockito:mockito-core",
                        "org.jboss.shrinkwrap.resolver:shrinkwrap-resolver-api"
                ).using(new CombinedStrategy(TransitiveStrategy.INSTANCE, new RejectDependenciesStrategy(false,
                                                                                                         "log4j:log4j",
                                                                                                         "mysql:mysql-connector-java",
                                                                                                         "javax.mail:mail"))).asFile();
        return libraries;
    }

    public static WebArchive createWorkbenchWar() {
        File[] libraries = getLibraries();

        final URL extension = Thread.currentThread().getContextClassLoader().getResource("META-INF/services/javax.enterprise.inject.spi.Extension");

        final WebArchive archive = ShrinkWrap.create(WebArchive.class, "workbench.war")
                .addClass(StandaloneControllerDynamicFeature.class)
                .addClass(StandaloneControllerFilter.class)

                .addClass(AsyncKieServerInstanceManager.class)

                .addClass(EmbeddedNotificationService.class)
                .addClass(RuleCapabilitiesServiceCDI.class)
                .addClass(RuntimeManagementServiceCDI.class)
                .addClass(SpecManagementServiceCDI.class)

                .addClass(ServerTemplateVFSStorage.class)
                .addClass(ServerTemplateOCPStorage.class)
                .addClass(ServerTemplateMigration.class)

                .addClass(ControllerExtension.class)
                .addClass(ControllerUtils.class)
                .addClass(EmbeddedController.class)
                .addClass(StandaloneController.class)

                .addClass(StandaloneControllerApplicationConfig.class)
                .addClass(StandaloneNotificationService.class)

                .addClass(KieServerEmbeddedControllerProducer.class)
                .addClass(KieServerStandaloneControllerProducer.class)

                .addClass(MockTestService.class)
                .addClass(MVELEvaluatorProducer.class)

                .addClass(AbstractControllerIT.class)
                .addClass(AbstractAutoControllerIT.class)
                .addClass(KieServerHealthCheckControllerImpl.class)
                .addClass(HealthCheckControllerBootstrap.class)

                .addClass(AppSetup.class)

                .addAsLibraries(libraries)

                .addAsResource(extension,
                               "META-INF/services/javax.enterprise.inject.spi.Extension")
                .addAsResource("META-INF/services/org.uberfire.java.nio.file.spi.FileSystemProvider")
                .addAsResource("security-policy.properties")

                .addAsManifestResource("jboss-all.xml")

                .addAsWebInfResource("META-INF/beans.xml",
                                     "beans.xml")
                .addAsWebInfResource("web.xml")
                .addAsWebInfResource("jboss-web.xml");

        LOGGER.debug("Workbench archive contents:\n{}", archive.toString(true));
        return archive;
    }

    public static WebArchive createKieServerWar() {
        // copy resources 
        String targetDir = System.getProperty("project.build.directory");
        try (InputStream file = new FileInputStream(targetDir + File.separator + "kie-server.war")) {
            return ShrinkWrap.create(ZipImporter.class, "kie-server.war").importFrom(file).as(WebArchive.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }

    public static WebArchive createKieServerControllerWar() {
        // copy resources 
        String targetDir = System.getProperty("project.build.directory");
        try (InputStream file = new FileInputStream(targetDir + File.separator + "kie-server-controller.war")) {
            return ShrinkWrap.create(ZipImporter.class, "kie-server-controller.war").importFrom(file).as(WebArchive.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }

    protected static String getWebSocketUrl(final URL baseURL) {
        return String.format("ws://%s:%s%swebsocket/controller",
                             baseURL.getHost(),
                             baseURL.getPort(),
                             baseURL.getPath());
    }

    protected static String getRestURL(final URL baseURL) {
        return baseURL + "rest/controller";
    }

    protected static void assertServerTemplateList(final ServerTemplateList serverTemplateList) {
        assertNotNull(serverTemplateList);
        assertNotNull(serverTemplateList.getServerTemplates());
        assertEquals(1, serverTemplateList.getServerTemplates().length);
        assertEquals(SERVER_TEMPLATE_ID, serverTemplateList.getServerTemplates()[0].getId());
    }


}
