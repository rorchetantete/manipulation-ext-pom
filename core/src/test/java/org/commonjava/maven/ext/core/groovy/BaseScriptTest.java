/*
 * Copyright (C) 2012 Red Hat, Inc. (jcasey@redhat.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.maven.ext.core.groovy;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.reflect.FieldUtils;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectRef;
import org.commonjava.maven.ext.common.ManipulationException;
import org.commonjava.maven.ext.common.model.Project;
import org.commonjava.maven.ext.core.ManipulationManager;
import org.commonjava.maven.ext.core.ManipulationSession;
import org.commonjava.maven.ext.core.fixture.TestUtils;
import org.commonjava.maven.ext.core.impl.FinalGroovyManipulator;
import org.commonjava.maven.ext.core.impl.InitialGroovyManipulator;
import org.commonjava.maven.ext.core.state.VersioningState;
import org.commonjava.maven.ext.io.FileIO;
import org.commonjava.maven.ext.io.PomIO;
import org.commonjava.maven.ext.io.resolver.GalleyInfrastructure;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class BaseScriptTest
{
    private static final String RESOURCE_BASE = "properties/";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public final SystemOutRule systemRule = new SystemOutRule().enableLog();//.muteForSuccessfulTests();

    @Test
    public void testGroovyAnnotation() throws Exception
    {
        // Locate the PME project pom file. Use that to verify inheritance tracking.
        final File groovy = new File( TestUtils.resolveFileResource( RESOURCE_BASE, "" )
                                               .getParentFile()
                                               .getParentFile()
                                               .getParentFile()
                                               .getParentFile(), "integration-test/src/it/setup/depMgmt1/Sample.groovy" );
        final File projectroot = new File( TestUtils.resolveFileResource( RESOURCE_BASE, "" )
                                                    .getParentFile()
                                                    .getParentFile()
                                                    .getParentFile()
                                                    .getParentFile(), "pom.xml" );
        PomIO pomIO = new PomIO();
        List<Project> projects = pomIO.parseProject( projectroot );
        ManipulationManager m = new ManipulationManager( Collections.emptyMap(), Collections.emptyMap(), null );
        ManipulationSession ms = TestUtils.createSession( null );
        m.init( ms );

        Project root = projects.stream().filter( p -> p.getProjectParent() == null ).findAny().orElse( null );
        logger.info( "Found project root " + root );

        InitialGroovyManipulator gm = new InitialGroovyManipulator( null, null );
        gm.init( ms );
        TestUtils.executeMethod( gm, "applyGroovyScript", new Class[] { List.class, Project.class, File.class },
                                 new Object[] { projects, root, groovy } );
        assertTrue( systemRule.getLog().contains( "BASESCRIPT" ) );
    }

    @Test
    public void testGroovyAnnotationIgnore() throws Exception
    {
        // Locate the PME project pom file. Use that to verify inheritance tracking.
        final File groovy = new File( TestUtils.resolveFileResource( RESOURCE_BASE, "" )
                                               .getParentFile()
                                               .getParentFile()
                                               .getParentFile()
                                               .getParentFile(), "integration-test/src/it/setup/depMgmt1/Sample.groovy" );
        final File projectroot = new File( TestUtils.resolveFileResource( RESOURCE_BASE, "" )
                                                    .getParentFile()
                                                    .getParentFile()
                                                    .getParentFile()
                                                    .getParentFile(), "pom.xml" );

        PomIO pomIO = new PomIO();
        List<Project> projects = pomIO.parseProject( projectroot );
        ManipulationManager m = new ManipulationManager( Collections.emptyMap(), Collections.emptyMap(), null );
        ManipulationSession ms = TestUtils.createSession( null );
        m.init( ms );

        Project root = projects.stream().filter( p -> p.getProjectParent() == null ).findAny().orElse( null );
        logger.info( "Found project root " + root );

        FinalGroovyManipulator gm = new FinalGroovyManipulator( null, null );
        gm.init( ms );
        TestUtils.executeMethod( gm, "applyGroovyScript", new Class[] { List.class, Project.class, File.class },
                                 new Object[] { projects, root, groovy } );

        assertTrue( systemRule.getLog().contains( "Ignoring script" ) );
        assertFalse( systemRule.getLog().contains( "BASESCRIPT" ) );
    }

    @Test
    public void testInlineProperty() throws Exception
    {
        // Locate the PME project pom file. Use that to verify inheritance tracking.
        final File projectroot = new File( TestUtils.resolveFileResource( RESOURCE_BASE, "" )
                                                    .getParentFile()
                                                    .getParentFile()
                                                    .getParentFile()
                                                    .getParentFile(), "pom.xml" );
        PomIO pomIO = new PomIO();
        List<Project> projects = pomIO.parseProject( projectroot );
        ManipulationManager m = new ManipulationManager( Collections.emptyMap(), Collections.emptyMap(), null );
        ManipulationSession ms = TestUtils.createSession( null );
        m.init( ms );

        Project root = projects.stream().filter( p -> p.getProjectParent() == null ).findAny().orElseThrow(Exception::new);

        logger.info( "Found project root {}", root );

        BaseScript bs = new BaseScript()
        {
            @Override
            public Object run()
            {
                return null;
            }
        };
        bs.setValues( null, ms, projects, root, null );

        bs.inlineProperty( root, SimpleProjectRef.parse( "org.commonjava.maven.atlas:atlas-identities" ) );
        bs.inlineProperty( root, SimpleProjectRef.parse( "org.commonjava.maven.galley:*" ) );

        assertEquals( "0.17.1", root.getModel()
                                    .getDependencyManagement()
                                    .getDependencies()
                                    .stream()
                                    .filter( d -> d.getArtifactId().equals( "atlas-identities" ) )
                                    .findFirst()
                                    .orElseThrow(Exception::new)
                                    .getVersion() );
        assertEquals( 5, root.getModel()
                                    .getDependencyManagement()
                                    .getDependencies()
                                    .stream()
                                    .filter( d -> d.getGroupId().equals( "org.commonjava.maven.galley" ) )
                                    .filter( d -> d.getVersion().equals( "0.16.3" ) )
                                    .count() );


        bs.inlineProperty( root, "jacksonVersion" );

        assertFalse( root.getModel()
                         .getDependencyManagement()
                         .getDependencies()
                         .stream()
                         .filter( d -> d.getArtifactId().equals( "jackson-databind" ) )
                         .findFirst()
                         .orElseThrow(Exception::new)
                         .getVersion()
                         .contains( "$" ) );


    }

    @Test
    public void testGroovyOverrideProperties() throws Exception
    {
        // Locate the PME project pom file. Use that to verify inheritance tracking.
        final File groovy = TestUtils.resolveFileResource( "", "PropertyOverride.groovy" );
        final File projectroot = new File( TestUtils.resolveFileResource( RESOURCE_BASE, "" )
                                                    .getParentFile()
                                                    .getParentFile()
                                                    .getParentFile()
                                                    .getParentFile(), "pom.xml" );

        List<Project> projects = new PomIO().parseProject( projectroot );

        Properties userProperties = new Properties(  );
        userProperties.setProperty( "versionIncrementalSuffix", "rebuild" );
        ManipulationSession session = TestUtils.createSession( userProperties );

        VersioningState state = session.getState( VersioningState.class );
        assertNotNull( state );
        assertTrue( state.isEnabled() );
        assertEquals( "rebuild", state.getIncrementalSerialSuffix() );
        assertNull( state.getSuffix() );


        Project root = projects.stream().filter( p -> p.getProjectParent() == null ).findAny().orElse( null );
        logger.info( "Found project root " + root );

        InitialGroovyManipulator gm = new InitialGroovyManipulator( null, null );
        gm.init( session );
        TestUtils.executeMethod( gm, "applyGroovyScript", new Class[] { List.class, Project.class, File.class },
                                 new Object[] { projects, root, groovy } );

        assertTrue( systemRule.getLog().contains( "BASESCRIPT" ) );
        assertTrue( systemRule.getLog().contains( "STAGE FIRST" ) );
        assertTrue( systemRule.getLog().contains( "MODELIO null" ) );
        assertEquals( "rebuild-5", state.getSuffix() );
    }


    @Test
    public void testGroovyExceptions() throws Exception
    {
        // Locate the PME project pom file. Use that to verify inheritance tracking.
        final File groovy = TestUtils.resolveFileResource( "", "GroovyExceptions.groovy" );
        final File pom = new File( TestUtils.resolveFileResource( RESOURCE_BASE, "" )
                                            .getParentFile()
                                            .getParentFile()
                                            .getParentFile()
                                            .getParentFile(), "pom.xml" );
        final File tmpFolder = temporaryFolder.newFolder();
        FileUtils.copyFileToDirectory( pom, tmpFolder);


        List<Project> projects = new PomIO().parseProject( new File (tmpFolder, "pom.xml" ));

        Properties userProperties = new Properties();
        userProperties.setProperty( "versionIncrementalSuffix", "rebuild" );
        userProperties.setProperty( "groovyScripts", "file://" + groovy.getAbsolutePath() );
        ManipulationSession session = TestUtils.createSession( userProperties );

        VersioningState state = session.getState( VersioningState.class );
        assertNotNull( state );
        assertTrue( state.isEnabled() );
        assertEquals( "rebuild", state.getIncrementalSerialSuffix() );
        assertNull( state.getSuffix() );

        Project root = projects.stream().filter( p -> p.getProjectParent() == null ).findAny().orElse( null );
        logger.info( "Found project root " + root );

        final DefaultContainerConfiguration config = new DefaultContainerConfiguration();

        config.setClassPathScanning( PlexusConstants.SCANNING_ON );
        config.setComponentVisibility( PlexusConstants.GLOBAL_VISIBILITY );
        config.setName( "PME-TEST" );

        final PlexusContainer container = new DefaultPlexusContainer( config );
        InitialGroovyManipulator gm = container.lookup( InitialGroovyManipulator.class );
        FileIO fileIO = new FileIO(
                        new GalleyInfrastructure( session, null).init( null, null, temporaryFolder.newFolder( "cache-dir" ) )) ;
        // Update the groovy manipulator with fileIO with a temporary folder for the cache directory.
        FieldUtils.writeField( gm, "fileIO", fileIO, true );

        gm.init( session );

        session.getUserProperties().setProperty( "manipExcep", "true" );
        try
        {
            gm.applyChanges( projects );
            fail("No exception thrown");
        }
        catch ( ManipulationException ex )
        {
            assertTrue( ex.getCause() == null && ex.getMessage().contains( "Manip Except" ) );
        }

        session.getUserProperties().setProperty( "manipExcep", "false" );
        try
        {
            gm.applyChanges( projects );
            fail("No exception thrown");
        }
        catch ( ManipulationException ex )
        {
            assertTrue( ex.getMessage().contains( "Problem running script" ) );
            assertTrue( ex.getCause().getMessage().contains( "IO problems" ) );
        }
    }
}
