package com.example.advertiseranalyzerwithlogs

import android.util.Log

object HammingEcc {
    private const val TAG = "HammingEcc"

    fun bitsToBytes(bits: List<UByte>): List<UByte> {
        val bytes = mutableListOf<UByte>()
        val byteCount = (bits.size + 7) / 8
        for (byteNum in 0 until byteCount) {
            var byteValue: UByte = 0u
            for (bit in 0..7) {
                val bitNum = byteNum * 8 + bit
                if (bits[bitNum] == 1u.toUByte()) {
                    byteValue = (byteValue + (1 shl bit).toUByte()).toUByte()
                }
            }
            bytes.add(byteValue)
        }
        return bytes
    }

    fun bytesToBits(bytes: List<UByte>): List<UByte> {
        val bits = mutableListOf<UByte>()
        for (byte in bytes) {
            for (bit in 0..7) {
                val bitVal: UByte =
                    if (byte and ((1 shl bit).toUByte()) > 0u) 1u
                    else 0u
                bits.add(bitVal)
            }
        }
        return bits
    }

    fun decodeBits(inputBits: List<UByte>): List<UByte>? {
        val outputBits = mutableListOf<UByte>()
        var ss: Int
        var error = 0
        var parityBitsRemoved = 0
        val workBits = inputBits.toMutableList()
        val extraParity = workBits.removeLast()

        val length = workBits.size
        var parityCount = 0

        var pos = 0
        while ((inputBits.size - parityCount) > ((1 shl pos) - (pos + 1))) {
            parityCount += 1
            pos += 1
        }

        // checking whether there are any errors
        var i = 0
        while (i < parityCount) {
            var count = 0
            val position = 1 shl i
            ss = position - 1
            while (ss < length) {
                var sss = ss
                while (sss < ss + position) {
                    if (sss < workBits.size && workBits[sss] == (1u).toUByte()) {
                        count += 1
                    }
                    sss += 1
                }
                ss += 2 * position
            }
            if (count % 2 != 0) {
                error += position
            }
            i += 1
        }

        // Correct errors
        if (error != 0) {
            if (error >= workBits.size) {
                // Too many errors. We do not want to cause an index out of range
                return null
            }
            if (workBits[error - 1] == (1u).toUByte()) {
                workBits[error - 1] = 0u
            } else {
                workBits[error - 1] = 1u
            }
            var k = 0
            while (k < length) {
                if (k == (1 shl parityBitsRemoved) - 1) {
                    parityBitsRemoved += 1
                } else {
                    if (workBits.size > k) {
                        outputBits.add(workBits[k])
                    } else {
                        outputBits.add(0u)
                    }
                }
                k += 1
            }
        } else {
            var j = 0
            while (j < length) {
                if (j == (1 shl parityBitsRemoved) - 1) {
                    parityBitsRemoved += 1
                } else {
                    outputBits.add(workBits[j])
                }
                j += 1
            }

        }

        // Finally one more parity check against the corrected bits.  If it doesn't match
        // we know there was more than one error and we cannot correct
        var parity: UByte = 0u
        for (bit in outputBits) {
            parity = parity or bit
        }
        if (parity != extraParity) {
            Log.e(TAG, "There were more than two errors.  Cannot decode")
            return null
        }
        return outputBits
    }
}