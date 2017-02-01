//
//  ========================================================================
//  Copyright (c) 1995-2017 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.start;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.regex.Matcher;

import org.eclipse.jetty.start.Props.Prop;

/**
 * Simple Start .INI handler
 */
public class StartIni extends TextFile
{
    private Path basedir;

    public StartIni(Path file) throws IOException
    {
        super(file);
    }

    @Override
    public void addUniqueLine(String line)
    {
        if (line.startsWith("--module="))
        {
            int idx = line.indexOf('=');
            String value = line.substring(idx + 1);
            for (String part : value.split(","))
            {
                super.addUniqueLine("--module=" + expandBaseDir(part));
            }
        }
        else
        {
            super.addUniqueLine(expandBaseDir(line));
        }
    }

    private String expandBaseDir(String line)
    {
        if (line == null)
        {
            return line;
        }

        return line.replace("${start.basedir}",basedir.toString());
    }

    @Override
    public void init()
    {
        try
        {
            basedir = getFile().getParent().toRealPath();
        }
        catch (IOException e)
        {
            basedir = getFile().getParent().normalize().toAbsolutePath();
        }
    }

    public Path getBaseDir()
    {
        return basedir;
    }

    public void update(BaseHome baseHome,Props props) throws IOException
    {
        String update = getFile().getFileName().toString();
        update = update.substring(0,update.lastIndexOf("."));
        String source = baseHome.toShortForm(getFile());
        
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(getFile(),StandardCharsets.UTF_8,StandardOpenOption.TRUNCATE_EXISTING,StandardOpenOption.CREATE)))
        {
            for (String line : getAllLines())
            {
                Matcher m = Module.SET_PROPERTY.matcher(line);
                if (m.matches() && m.groupCount()==3)
                {
                    String name = m.group(2);
                    String value = m.group(3);
                    Prop p = props.getProp(name);
                    if (p!=null && ("#".equals(m.group(1)) || !value.equals(p.value)))
                    {
                        StartLog.info("%-15s property updated %s=%s",update,name,p.value);
                        writer.printf("%s=%s%n",name,p.value);
                    }
                    else
                    {
                        writer.println(line);
                    }
                }
                else
                {
                    writer.println(line);
                }
            }
        }

        StartLog.info("%-15s updated %s",update,source);
    }
}
