/*
 * Copyright (c) 2023, Arm Limited. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package compiler.vectorapi;

import compiler.lib.ir_framework.*;

import java.util.Random;

import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.LongVector;
import jdk.incubator.vector.ShortVector;
import jdk.incubator.vector.VectorSpecies;
import jdk.incubator.vector.VectorOperators;

import jdk.test.lib.Asserts;
import jdk.test.lib.Utils;

/**
 * @test
 * @bug 8301739
 * @key randomness
 * @library /test/lib /
 * @requires os.arch=="aarch64"
 * @summary AArch64: Add optimized rules for vector compare with immediate for SVE
 * @modules jdk.incubator.vector
 *
 * @run driver compiler.vectorapi.VectorCompareWithImmTest
 */

public class VectorCompareWithImmTest {
    private static final VectorSpecies<Byte> B_SPECIES = ByteVector.SPECIES_PREFERRED;
    private static final VectorSpecies<Short> S_SPECIES = ShortVector.SPECIES_PREFERRED;
    private static final VectorSpecies<Integer> I_SPECIES = IntVector.SPECIES_PREFERRED;
    private static final VectorSpecies<Long> L_SPECIES = LongVector.SPECIES_PREFERRED;

    private static final int LENGTH = 1024;
    private static final Random RD = Utils.getRandomInstance();

    private static byte[] ba;
    private static byte b_imm5;
    private static byte b_imm7;
    private static byte b_out_range;
    private static short[] sa;
    private static short s_imm5;
    private static short s_imm7;
    private static short s_out_range;
    private static int[] ia;
    private static int i_imm5;
    private static int i_imm7;
    private static int i_out_range;
    private static long[] la;
    private static long l_imm5;
    private static long l_imm7;
    private static long l_out_range;

    static {
        ba = new byte[LENGTH];
        sa = new short[LENGTH];
        ia = new int[LENGTH];
        la = new long[LENGTH];

        b_imm5 = (byte) RD.nextInt(16); // [0, 16)
        b_imm7 = (byte) ((int)32 + RD.nextInt(16)); // [32, 48)
        b_out_range = (byte) (RD.nextInt(16) - (int)127); // [-127, -111)

        for (int i = 0; i < LENGTH; i++) {
            ba[i] = (byte) RD.nextInt(25);
            sa[i] = (short) RD.nextInt(25);
            ia[i] = RD.nextInt(25);
            la[i] = RD.nextLong(25);
        }
    }

    interface ByteOp {
        boolean apply(byte a);
    }

    private static void assertArrayEquals(byte[] a, boolean[] r, ByteOp f) {
        for (int i = 0; i < LENGTH; i++) {
            Asserts.assertEquals(f.apply(a[i]), r[i]);
        }
    }

    @Test
    @IR(counts = { IRNode.VMASK_CMP_IMM5_I_SVE, ">= 1" })
    public static void testByteVectorGreaterThanImm5() {
        boolean[] r = new boolean[LENGTH];
        for (int i = 0; i < LENGTH; i += B_SPECIES.length()) {
            ByteVector av = ByteVector.fromArray(B_SPECIES, ba, i);
            av.compare(VectorOperators.EQ, b_imm5).intoArray(r, i);
        }

        assertArrayEquals(ba, r, (a) -> (a == b_imm5 ? true : false));
    }

    public static void main(String[] args) {
        TestFramework testFramework = new TestFramework();
        testFramework.setDefaultWarmup(10000)
                    .addFlags("--add-modules=jdk.incubator.vector")
                    .addFlags("-XX:UseSVE=0")
                    .start();
    }
}