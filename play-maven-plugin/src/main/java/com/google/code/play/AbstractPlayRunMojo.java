/*
 * Copyright 2010-2016 Grzegorz Slowikowski
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.code.play;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.tools.ant.taskdefs.Java;

/**
 * Base class for Play&#33; server synchronously starting ("run" and "test") mojos.
 * 
 * @author <a href="mailto:gslowikowski@gmail.com">Grzegorz Slowikowski</a>
 */
public abstract class AbstractPlayRunMojo
    extends AbstractPlayServerMojo
{

    /**
     * Allows the server startup to be skipped.
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play.runSkip", defaultValue = "false" )
    private boolean runSkip;

    /**
     * Run in forked Java process.
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play.runFork", defaultValue = "true" )
    private boolean runFork;

    protected abstract String getPlayId();

    @Override
    protected void internalExecute()
        throws MojoExecutionException, MojoFailureException, IOException
    {
        if ( runSkip )
        {
            getLog().info( "Skipping execution" );
            return;
        }
        
        String checkMessage = playModuleNotApplicationCheck();
        if ( checkMessage != null )
        {
            getLog().info( checkMessage );
            return;
        }

        File baseDir = project.getBasedir();
        
        File pidFile = new File( baseDir, "server.pid" );
        if ( pidFile.exists() )
        {
            throw new MojoExecutionException( String.format( "Play! Server already started (\"%s\" file found)",
                                                             pidFile.getName() ) );
        }

        String playId = getPlayId();

        ConfigurationParser configParser =  getConfiguration( playId );

        Java javaTask = prepareAntJavaTask( configParser, playId, runFork );
        javaTask.setFailonerror( true );

        JavaRunnable runner = new JavaRunnable( javaTask );
        Thread t = new Thread( runner, "Play! Server runner" );
        getLog().info( "Launching Play! Server" );
        t.start();
        try
        {
            t.join(); // waiting for Ctrl+C if forked, joins immediately if not forking
        }
        catch ( InterruptedException e )
        {
            throw new MojoExecutionException( "Thread interrupted", e );
        }
        Exception runException = runner.getException();
        if ( runException != null )
        {
            throw new MojoExecutionException( "Play! Server start error: " + runException.getMessage(), runException );
        }
        
        if ( !runFork )
        {
            while ( true ) // wait for Ctrl+C
            {
                try
                {
                    Thread.sleep( 10000 );
                }
                catch ( InterruptedException e )
                {
                    throw new MojoExecutionException( "Thread interrupted", e );
                }
            }
        }
    }

}
