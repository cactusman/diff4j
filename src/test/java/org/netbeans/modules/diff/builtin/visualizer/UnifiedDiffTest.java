/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2007 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */
package org.netbeans.modules.diff.builtin.visualizer;

import com.cloudbees.diff.provider.BuiltInDiffProvider;
import com.cloudbees.diff.Diff;
import junit.framework.TestCase;

import java.io.*;

import junit.framework.TestSuite;

/**
 * Takes all files from a directory and compares them all with each other comparing results with external diff engine. 
 *
 * @author Maros Sandor
 */
public class UnifiedDiffTest extends TestCase {
    
    private BuiltInDiffProvider diffProvider;
    private final File[] toDiff;

    public UnifiedDiffTest(File[] toDiff) {
        super("Testing: " + toDiff[0].getName() + " <-> " + toDiff[1].getName());
        this.toDiff = toDiff;
    }

    protected void setUp() throws Exception {
        diffProvider = new BuiltInDiffProvider();
        diffProvider.setTrimLines(false);
    }

    public static TestSuite suite() {
        TestSuite r = new TestSuite();

        File dataRootDir = new File(UnifiedDiffTest.class.getResource("/data/unidiff/1.txt").getPath()).getParentFile().getParentFile();
        File[] testFiles = new File(dataRootDir, "unidiff").listFiles();
        int idx0=0, idx1=0;

        for (;;) {
            if (++idx1 == idx0) idx1++;
            if (idx1 >= testFiles.length) {
                if (++idx0 >= testFiles.length) break;
                idx1 = 0;
            }
            final File[] toDiff = new File [] { testFiles[idx0], testFiles[idx1] };

            r.addTest(new UnifiedDiffTest(toDiff));
        }

        return r;
    }

    @Override
    protected void runTest() throws Throwable {
        String internalDiff = getInternalDiff(toDiff);
        String externalDiff = getExternalDiff(toDiff);
        if (!diffsEqual(internalDiff, externalDiff)) {
            saveFailure(externalDiff, internalDiff);
            fail("Diff failed: " + toDiff[0].getName() + " <-> " + toDiff[1].getName());
        }
    }

    private void saveFailure(String externalDiff, String internalDiff) throws IOException {
        try {
            copyStreamsCloseAll(new FileWriter("./diff-unit-failure-external.txt"), new StringReader(externalDiff));
            copyStreamsCloseAll(new FileWriter("./diff-unit-failure-internal.txt"), new StringReader(internalDiff));
        } catch (Exception e) {
            // ignore save errors
        }
    }

    private boolean diffsEqual(String sa, String sb) throws IOException {
        // ignore header
        int idxa = sa.indexOf("@@");
        int idxb = sb.indexOf("@@");
        if (idxa == -1 && idxb == -1) return true;
        if (idxa == -1 || idxb == -1) return false;
        sa = sa.substring(idxa);
        sb = sb.substring(idxb);
        return sa.equals(sb);
    }

    private String getInternalDiff(File[] toDiff) throws IOException {
        Reader r1 = new FileReader(toDiff[0]);
        Reader r2 = new FileReader(toDiff[1]);
        Diff diffs = diffProvider.computeDiff(r1, r2);
        r1.close();
        r2.close();
        r1 = new FileReader(toDiff[0]);
        r2 = new FileReader(toDiff[1]);

        return diffs.toUnifiedDiff("name1","name2",r1,r2,3);
    }

    private String getExternalDiff(File[] toDiff) throws IOException {
        Process p = Runtime.getRuntime().exec(new String [] { "diff", "-u", toDiff[0].getAbsolutePath(), toDiff[1].getAbsolutePath() });
        InputStreamReader isr = new InputStreamReader(p.getInputStream());
        StringWriter sw = new StringWriter();
        copyStreamsCloseAll(sw, isr);
        return sw.toString();
    }

    private static void copyStreamsCloseAll(Writer writer, Reader reader) throws IOException {
        char [] buffer = new char[4096];
        int n;
        while ((n = reader.read(buffer)) != -1) {
            writer.write(buffer, 0, n);
        }
        writer.close();
        reader.close();
    }
}
