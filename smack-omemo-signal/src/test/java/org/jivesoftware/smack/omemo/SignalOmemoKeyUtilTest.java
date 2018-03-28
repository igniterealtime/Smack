/**
 *
 * Copyright 2017 Paul Schaub
 *
 * This file is part of smack-omemo-signal.
 *
 * smack-omemo-signal is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 */
package org.jivesoftware.smack.omemo;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNotSame;
import static junit.framework.TestCase.assertTrue;

import java.util.HashMap;
import java.util.Iterator;

import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smack.test.util.TestUtils;

import org.jivesoftware.smackx.omemo.element.OmemoBundleVAxolotlElement;
import org.jivesoftware.smackx.omemo.exceptions.CorruptedOmemoKeyException;
import org.jivesoftware.smackx.omemo.internal.OmemoDevice;
import org.jivesoftware.smackx.omemo.provider.OmemoBundleVAxolotlProvider;
import org.jivesoftware.smackx.omemo.signal.SignalOmemoKeyUtil;

import org.junit.Test;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;
import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.state.PreKeyBundle;
import org.whispersystems.libsignal.state.PreKeyRecord;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;

/**
 * Test SignalOmemoKeyUtil methods.
 *
 * @author Paul Schaub
 */
public class SignalOmemoKeyUtilTest extends SmackTestSuite {

    private final SignalOmemoKeyUtil keyUtil = new SignalOmemoKeyUtil();

    @Test
    public void generateOmemoIdentityKeyPairTest() {
        IdentityKeyPair ikp = keyUtil.generateOmemoIdentityKeyPair();
        assertNotNull("IdentityKeyPair must not be null.", ikp);
        assertNotNull("PrivateKey must not be null.", ikp.getPrivateKey());
        assertNotNull("PublicKey must not be null.", ikp.getPublicKey());
    }

    @Test
    public void omemoIdentityKeyPairSerializationTest() throws CorruptedOmemoKeyException {
        IdentityKeyPair ikp = keyUtil.generateOmemoIdentityKeyPair();
        byte[] bytes = keyUtil.identityKeyPairToBytes(ikp);
        assertNotNull("serialized identityKeyPair must not be null.",
                bytes);
        assertNotSame("serialized identityKeyPair must not be of length 0.",
                0, bytes.length);

        IdentityKeyPair ikp2 = keyUtil.identityKeyPairFromBytes(bytes);
        assertTrue("Deserialized IdentityKeyPairs PublicKey must equal the originals one.",
                    ikp.getPublicKey().equals(ikp2.getPublicKey()));
    }

    @Test
    public void omemoIdentityKeySerializationTest() throws CorruptedOmemoKeyException {
        IdentityKey k = keyUtil.generateOmemoIdentityKeyPair().getPublicKey();
        assertEquals("Deserialized IdentityKey must equal the original one.", k,
                keyUtil.identityKeyFromBytes(keyUtil.identityKeyToBytes(k)));
    }

    @Test
    public void generateOmemoPreKeysTest() {
        HashMap<Integer, PreKeyRecord> pks =
                keyUtil.generateOmemoPreKeys(1, 20);
        assertTrue("There must be 20 preKeys.", pks.size() == 20);
        assertTrue("PreKey ids must be within boundaries [1, 20]", pks.keySet().contains(1) && pks.keySet().contains(20));
    }

    @Test
    public void generateOmemoSignedPreKeyTest() throws CorruptedOmemoKeyException {
        IdentityKeyPair ikp = keyUtil.generateOmemoIdentityKeyPair();
        SignedPreKeyRecord spk = keyUtil.generateOmemoSignedPreKey(ikp, 1);
        assertNotNull("SignedPreKey must not be null.", spk);
        assertEquals("SignedPreKeyId must match.", 1, spk.getId());
        assertEquals("singedPreKeyId must match here also.", 1, keyUtil.signedPreKeyIdFromKey(spk));
    }

    @Test
    public void getFingerprintTest() {
        IdentityKeyPair ikp = keyUtil.generateOmemoIdentityKeyPair();
        IdentityKey ik = ikp.getPublicKey();
        assertTrue("Length of fingerprint must be 64.",
                keyUtil.getFingerprint(ik).length() == 64);
    }

    @Test
    public void addressToDeviceTest() throws XmppStringprepException {
        SignalProtocolAddress address = new SignalProtocolAddress("test@server.tld", 1337);
        OmemoDevice device = keyUtil.addressAsOmemoDevice(address);
        assertEquals(device, new OmemoDevice(JidCreate.bareFrom("test@server.tld"), 1337));
    }

    @Test
    public void deviceToAddressTest() throws XmppStringprepException {
        OmemoDevice device = new OmemoDevice(JidCreate.bareFrom("test@server.tld"), 1337);
        SignalProtocolAddress address = keyUtil.omemoDeviceAsAddress(device);
        assertEquals(address, new SignalProtocolAddress("test@server.tld", 1337));
    }

    @Test
    public void bundlesFromOmemoBundleTest() throws Exception {
        OmemoDevice device = new OmemoDevice(JidCreate.bareFrom("test@test.tld"), 1337);
        String bundleXML = "<bundle xmlns='eu.siacs.conversations.axolotl'><signedPreKeyPublic signedPreKeyId='1'>BYKq4s6+plpjAAuCnGO+YFpLP71tMUPgj9ZZmkMSko4E</signedPreKeyPublic><signedPreKeySignature>TYMUtzWpc5USMCXStUrCbXFeHTOX3xkBTrU6/MuE/16s4ql1vRN0+JLtYPgZtTm3hb2dHwLA5BUzeTRGjSZwig==</signedPreKeySignature><identityKey>BY3AYRje4YBA6W4uuAXYNKzbII/UJbw7qE8kWHI15eti</identityKey><prekeys><preKeyPublic preKeyId='1'>BbzKUJbnqYW19h2dWCyLMbYEpF8r477Ukv9wqMayERQE</preKeyPublic><preKeyPublic preKeyId='2'>Beit9Pz31QxklV69BZ0qIxktnUO5TYAgHacFWDYsDnhd</preKeyPublic><preKeyPublic preKeyId='3'>BSlbqC8nOpG4TMqvZmCPr6TCPNRcuuoO8Fp2rLGwLFYz</preKeyPublic><preKeyPublic preKeyId='4'>BWYsJTsJLtmOgChiz4ilS/cgoEptnfv87tuvq5VpZFV+</preKeyPublic><preKeyPublic preKeyId='5'>BY/xq67AkvgIaUO1NbROJeG+r6CcpzByoKvpIaPYyaw/</preKeyPublic><preKeyPublic preKeyId='6'>BVRkNWaoocepKEqah95F1DG/uTE1iNEgIZ40wnGd39g/</preKeyPublic><preKeyPublic preKeyId='7'>BWMI2ivYBIziOiJsnxJHmiUNN1GcPs3vP/E4vn7hu10B</preKeyPublic><preKeyPublic preKeyId='8'>Bd7QSMnxJULdKHohRhxUW/DVVRhdaY9SSX16j+CJF8Yd</preKeyPublic><preKeyPublic preKeyId='9'>BSgQ8NXIkq9fZrtYEdV6qkz5EK7YXVRAiIAFaaDuwUZH</preKeyPublic><preKeyPublic preKeyId='10'>Bf9Q2r9P4P15GvIiaHWTEU5gLyk/A8ys6Pzz01pLuu9Z</preKeyPublic><preKeyPublic preKeyId='11'>BVU6/JKCXqaNa4ApbPFxYExxKuQKuRctk8a1brNcRbJU</preKeyPublic><preKeyPublic preKeyId='12'>BfFGHormRpE7x92Eo3IcZcyhxa1//lKyLCNLdlL5Gg1P</preKeyPublic><preKeyPublic preKeyId='13'>Bd/Je4PdYYJy+6gXrcy7CRqDxBHVgPKN9AOiGxpRX7gk</preKeyPublic><preKeyPublic preKeyId='14'>BVtdD2xyJnxPYNJPCT7sYdCXAoD7pMLgf27Dj0dU9vU3</preKeyPublic><preKeyPublic preKeyId='15'>BX41BkuSp/qGYDlEzsuE5Tlia1IjzmYsiZRcjAp8D2tq</preKeyPublic><preKeyPublic preKeyId='16'>BRY9W9zotVhB7DV2s/I7RYFzzg/Rok0AjU6ODs+iBUtF</preKeyPublic><preKeyPublic preKeyId='17'>Bb4DW8bURvMuh21PzHGqQlQm6eaI2S4pPLD482yV65IU</preKeyPublic><preKeyPublic preKeyId='18'>BSFOrkueqrJDACBIUDpaYiOV51fUuFit4dGYYkvV3Sty</preKeyPublic><preKeyPublic preKeyId='19'>BT402/OG5FLw2jt+cpYepykpoRVPbI+bWcUx42CqSlwx</preKeyPublic><preKeyPublic preKeyId='20'>BeMDEcZ23jnocObmU+esIhAGUvEVCyeiqq+n29Ex38Fw</preKeyPublic><preKeyPublic preKeyId='21'>BYUDDsKjORZTuZ1ImIIcwhL2peK1K+kTS+QhqCufoIRJ</preKeyPublic><preKeyPublic preKeyId='22'>BcC/x3Q3zZKv2DKaZlTWpM2Qzg8UogXJ2MmyKQzNI6RJ</preKeyPublic><preKeyPublic preKeyId='23'>Bad8sDrpoVujQTlenKtSfc7JbWlXq5MGDb71q+5DCo88</preKeyPublic><preKeyPublic preKeyId='24'>BYlAA5ZyhfiKLFE/U6lufiokNmQjGYP5eMCKhZsuv9BX</preKeyPublic><preKeyPublic preKeyId='25'>BbK+LNKsLizmJtd6iEd+QUDdBEgmxIylkTyAS2gxghEH</preKeyPublic><preKeyPublic preKeyId='26'>BZ+9oZGHWkRJXPnzT54+UPhQY0vpUdzGltMvneZHqfML</preKeyPublic><preKeyPublic preKeyId='27'>BRRXzcCruX3Gb+kbBodA9OaHcEx/XYT3dpwKK6hx8mYf</preKeyPublic><preKeyPublic preKeyId='28'>BTgeei2VCoKk3dBG0FP45UjDoJBV9wQiDn2pW9xwTMkS</preKeyPublic><preKeyPublic preKeyId='29'>BZHFWmtevdvuYAbMOpQ7nAAdv+oJxY+A7GFi2jU/PftP</preKeyPublic><preKeyPublic preKeyId='30'>BRn4+vobphaBHjOl4gYrVIPHEGMvsn63pbAVgdx69XQR</preKeyPublic><preKeyPublic preKeyId='31'>BaUv1tnXFTkJ2jiFT0vlUjH9upOASZHN4EmXGX9n9UAc</preKeyPublic><preKeyPublic preKeyId='32'>BU+13hmRR2dkuIqBKxItFFaIdnaAti3beOnmezR+/VtW</preKeyPublic><preKeyPublic preKeyId='33'>BbmmB27Q1B72qhxxW++CyrNHCy0UwiAOdkKOBUKCkyZ0</preKeyPublic><preKeyPublic preKeyId='34'>BemHNdH5VhufFn9n4qu6e1pVyYjn47ivQy1xHmQL6eh1</preKeyPublic><preKeyPublic preKeyId='35'>BSnbvvDgCRGpu/SkapLOe66hxxeJKw7U160d6vxUkYM6</preKeyPublic><preKeyPublic preKeyId='36'>BVaUjCB5ZhooG2umXa4CVu6BjmNDkkUUM19pzangbfEU</preKeyPublic><preKeyPublic preKeyId='37'>BZD+gzgJ4jXxjfJtMMuWvHJmr/f5vJ+u7vhH4y7KjYM3</preKeyPublic><preKeyPublic preKeyId='38'>BW3zmMGSm5jhMTpSjT8u0dsDnK2pXMRVPTr08xmh7vhJ</preKeyPublic><preKeyPublic preKeyId='39'>BSE7XKChX5zcJrJtoBTAVtUL/gB9iFFb2rE0fKj2b2UQ</preKeyPublic><preKeyPublic preKeyId='40'>BXVao8jlCDAeOMr4thch7T8Gl+7h2OhcihFAOqkmzf9M</preKeyPublic><preKeyPublic preKeyId='41'>BdPqg07COBd2OInhQqc1yCZbixd1CpEbpcG9NjbxGwRU</preKeyPublic><preKeyPublic preKeyId='42'>BTzmunAmQX61OaIlTYdfWQU3VtkVXdiLCcegUIOzg/hw</preKeyPublic><preKeyPublic preKeyId='43'>BfRxST8negQ2vxMQLufVXdOM/U5IPHCETGsV2uhkdz4H</preKeyPublic><preKeyPublic preKeyId='44'>BTlbSzgCQwwkjD/EbEWfcontJobg9u5Odqn/x9QAmu1j</preKeyPublic><preKeyPublic preKeyId='45'>BczhPhwuz7KQJW8KICaOgQ0J/+baVwptpqxOtwjFphQh</preKeyPublic><preKeyPublic preKeyId='46'>Bc0xu3QbrVWQDlIh2VdrfP/GowUF8CN5Q3iCpuabLhIK</preKeyPublic><preKeyPublic preKeyId='47'>BaiPlpNAjMviSv3n3tJ+8vAQS7IORAuYJz8pZ/k7Cdth</preKeyPublic><preKeyPublic preKeyId='48'>BUDuxRt02ajimnvq8BeBQEies6TNDs/E0uvZ7aLHBJAI</preKeyPublic><preKeyPublic preKeyId='49'>BQFVdgojx8r3LOjJbAk3CWhtCxU2DxFQHyoBewfJyTk9</preKeyPublic><preKeyPublic preKeyId='50'>BdMJqMd7Rkiu9tcmcG1TDU0XKoEHJYPK3FBfRScvqlBr</preKeyPublic><preKeyPublic preKeyId='51'>BSyfLhWFdFkUcyczOpIgwo5M5JrJEWWLBJLrOYZHlkYb</preKeyPublic><preKeyPublic preKeyId='52'>BeM0hOY/zjvwbGgFHTLAKplV2A57bKzJOd0qhkc22zk1</preKeyPublic><preKeyPublic preKeyId='53'>BRIohETGkJNWCDmZjnq7kgawbPWjjBaok4QMTSynT3Qc</preKeyPublic><preKeyPublic preKeyId='54'>BZykP11RVcyQQmYD+gxGYzL1aQlKce3+EzPZDunh0ftO</preKeyPublic><preKeyPublic preKeyId='55'>BYwQvPfzvB97+QcBfCR2YW1EOIDw8KW5FrGmhw4/JxlP</preKeyPublic><preKeyPublic preKeyId='56'>BYNXW8MBYvPvtsjo2LVUBy4JZdRfG1WKq2dNY8gt+OFe</preKeyPublic><preKeyPublic preKeyId='57'>BSXp4XWf7UkZnM5IK0nQf2/PqHqkMXBq9s/z0YRWUt44</preKeyPublic><preKeyPublic preKeyId='58'>BUzNkOEe1jnuoJ4sQpz9DeBojDr1qfpadPr6UbC9SSoz</preKeyPublic><preKeyPublic preKeyId='59'>BXONDctFe7rI0h5+erFwpp+LjU9MnVONIhpOsX+aiTQT</preKeyPublic><preKeyPublic preKeyId='60'>BRElBbzl1sPRtu3r7kQfjqzXn1LxwnRU7gpWxjVMrplK</preKeyPublic><preKeyPublic preKeyId='61'>BdGuy7iMtNqzmLOgG8QH63Jc22Mo7Tyquz4UkeT1F+8j</preKeyPublic><preKeyPublic preKeyId='62'>Bas8r4IYxDpWYCvwTE+esHgELip8d/C3BJP14W74RjJq</preKeyPublic><preKeyPublic preKeyId='63'>BTJCGy3cDLmpDqHaXE9NPaEs1kKibx4fNx4SmEc74xMs</preKeyPublic><preKeyPublic preKeyId='64'>BRLV4n9fTnOnt0omE4xfl9XsYlml78F7bs585qiWyAwN</preKeyPublic><preKeyPublic preKeyId='65'>BUWflPRltdUAfkQbFWjEbTDc5FBImnSAxZk/GYqyGwB1</preKeyPublic><preKeyPublic preKeyId='66'>BfKrwvFbKawM8Y18oPzXd8dNk821fZ3s2r+yXFrsLDlH</preKeyPublic><preKeyPublic preKeyId='67'>BbqXgiP75kNoQPZ6MYNUdLvepRLQc1EBm5ZYV9VW56Ef</preKeyPublic><preKeyPublic preKeyId='68'>BazAZ71zu++p6o0LAJNBJIgKSacrque4veToF850TpQW</preKeyPublic><preKeyPublic preKeyId='69'>BQptZxpQZugPAK9CMZnR3p+gF0rqYVihRnUIdWAmhMB9</preKeyPublic><preKeyPublic preKeyId='70'>Ba70cNznf57ndU6NY62paZcDTTOZmPPS8/JZqLyP+ZVr</preKeyPublic><preKeyPublic preKeyId='71'>BSDQwgSHsNjf3MOh4SRRd5jzq/kcjIlf6JEa1SoX06Bn</preKeyPublic><preKeyPublic preKeyId='72'>BQ1ATRmYMPCyNt8fu/GZ0UeAYWG+WtiDs0uDLsmklI4e</preKeyPublic><preKeyPublic preKeyId='73'>BSAofueQkVpDo+I4SoFMdC8S35EOvOn7zmyOG4stSy4B</preKeyPublic><preKeyPublic preKeyId='74'>BcpdJVI1JARw8QeKXhbsMIgFxQzTvMSuQeAyvdYfgFIX</preKeyPublic><preKeyPublic preKeyId='75'>BURqmjb1lZU66KyPBlCWrjBbISJyqgMW8OaJOchk39YL</preKeyPublic><preKeyPublic preKeyId='76'>BVcQm66sdtSBIYK9KymoaZnSvLQPNftBPi+BPfg20Vwh</preKeyPublic><preKeyPublic preKeyId='77'>BQDNPKib8FK5YquNUAzB7sirGjdj+El+HrOTlMr0w1om</preKeyPublic><preKeyPublic preKeyId='78'>BQ66K4ENDGMAlZc7AqcE9dodeeAWfGzSyRYMto57iGAX</preKeyPublic><preKeyPublic preKeyId='79'>BTnfRRbPKKBLyoV/BTeIZhkfs629J462AvxuE3pHgvca</preKeyPublic><preKeyPublic preKeyId='80'>Bfyu+Cln9QhDLWz1AqOuYgqkh78LROOk4g326gj378gX</preKeyPublic><preKeyPublic preKeyId='81'>BRZovbjk6iAtKaKGLvLWlGGml/SUhMtSJEgjrO4tWd9s</preKeyPublic><preKeyPublic preKeyId='82'>BZ6OUOFAbuIPTaOwy0qyA6zZ9uYyxskF6i7EXWNQr1Nr</preKeyPublic><preKeyPublic preKeyId='83'>BWV8bGYfPvLq7Dla1gEqZv3eFej2UzcMWvFOiwurY7AS</preKeyPublic><preKeyPublic preKeyId='84'>BSZQ8prazrspZeNKzJzZc0bp1PEs1odEHsI7PLYCUVQd</preKeyPublic><preKeyPublic preKeyId='85'>BbAYn4nIg9EjRh92dTHKfgrTC/oAU/92U2WkDtCS+fs1</preKeyPublic><preKeyPublic preKeyId='86'>BehKd0MHqJauFPVQsS37SIFwUXo0OOcMembkOhyMGPF8</preKeyPublic><preKeyPublic preKeyId='87'>BS7CeBYN+H0s+GwxIrUc5SmdMZEXTprVZD6RYoM+YyxK</preKeyPublic><preKeyPublic preKeyId='88'>BSBc48kcT2EN1Siv/hoX8ozuHSEfQXIS93SNY8+Jg7pz</preKeyPublic><preKeyPublic preKeyId='89'>Bdr7WFoKkG1m/CsTV7J2G9/yXV1pOupqPyU7Rs5FjVoJ</preKeyPublic><preKeyPublic preKeyId='90'>Bc+i4mSLKDMm+ZxkcWMdVdM4p/MlBOFQLb+NF9j4QxlT</preKeyPublic><preKeyPublic preKeyId='91'>BfQslqyOk1QwcdrJRJVUvlHUYGJc115O17sb5HIP7GE2</preKeyPublic><preKeyPublic preKeyId='92'>BbHvfsMnJu2y60YmI509hoUkgGN1UqrOMLMwoC8TDqp6</preKeyPublic><preKeyPublic preKeyId='93'>BVQDMiH5KfKHZLbhTwXxR4RdsADov1gD2elDd6SO+hIQ</preKeyPublic><preKeyPublic preKeyId='94'>BaNnLStoh3EygkLfA9tjULQYg6X7L/n1jNQeaFKaGjsa</preKeyPublic><preKeyPublic preKeyId='95'>Bffy5atUJ49XgzsxXMiAopLhTU0rJtGIId0g+kggLBYa</preKeyPublic><preKeyPublic preKeyId='96'>Bb5fC0qp2eJq8HvJVkf7MIJk+eBZ3TVasvwCn8t4MhEh</preKeyPublic><preKeyPublic preKeyId='97'>Bb8H48LSq/nxpOKovpLVYw8X3mIJM7JMk3yYgFUKdL0p</preKeyPublic><preKeyPublic preKeyId='98'>BTfHeYAsa2hl/aoA3wslmL9RT+O26P6OWs0J2dif5o5p</preKeyPublic><preKeyPublic preKeyId='99'>Bf7u5QrY3Wrn0PYaRri5nDL6p6iNHFLSk6781wys0hkp</preKeyPublic><preKeyPublic preKeyId='100'>BSBryRkeNrvLgGJgh95g9oWLmrptWVPIGPSzoXrVNlAd</preKeyPublic></prekeys></bundle>";
        OmemoBundleVAxolotlElement bundle = new OmemoBundleVAxolotlProvider().parse(TestUtils.getParser(bundleXML));
        HashMap<Integer, PreKeyBundle> bundles = keyUtil.BUNDLE.bundles(bundle, device);

        assertEquals("There must be 100 bundles in the HashMap.", 100, bundles.size());
        assertNotNull(keyUtil.BUNDLE.identityKey(bundle));

        Iterator<Integer> it = bundles.keySet().iterator();
        while (it.hasNext()) {
            assertNotNull(keyUtil.BUNDLE.preKeyPublic(bundle, it.next()));
        }

        assertEquals(1, keyUtil.BUNDLE.signedPreKeyId(bundle));
        assertNotNull(keyUtil.BUNDLE.signedPreKeyPublic(bundle));
        assertNotNull(keyUtil.BUNDLE.signedPreKeySignature(bundle));
    }
}
