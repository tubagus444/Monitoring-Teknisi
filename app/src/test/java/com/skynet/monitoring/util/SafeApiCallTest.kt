package com.skynet.monitoring.util

import com.google.gson.Gson
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class SafeApiCallTest {

    private val gson = Gson()
    private val json = "application/json".toMediaType()

    private fun errorBody(content: String) = content.toResponseBody(json)

    @Test
    fun `sukses mengembalikan body`() = runTest {
        val result = safeApiCall(gson) { Response.success("ok") }
        assertTrue(result.isSuccess)
        assertEquals("ok", result.getOrNull())
    }

    @Test
    fun `body kosong pada response sukses dianggap gagal`() = runTest {
        val result = safeApiCall<String?>(gson) { Response.success<String?>(null) }
        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull() as ApiException
        assertEquals("Respons kosong dari server", ex.message)
        assertNull(ex.code)
    }

    @Test
    fun `401 dengan message backend memakai message dan kode`() = runTest {
        val result = safeApiCall<String>(gson) {
            Response.error(401, errorBody("""{"message":"Email atau password salah"}"""))
        }
        val ex = result.exceptionOrNull() as ApiException
        assertEquals("Email atau password salah", ex.message)
        assertEquals(401, ex.code)
    }

    @Test
    fun `422 validasi mengambil pesan pertama dari errors`() = runTest {
        val body = """{"message":"Data tidak valid","errors":{"email":["Email wajib diisi","Email tidak valid"]}}"""
        val result = safeApiCall<String>(gson) { Response.error(422, errorBody(body)) }
        val ex = result.exceptionOrNull() as ApiException
        assertEquals("Email wajib diisi", ex.message)
        assertEquals(422, ex.code)
    }

    @Test
    fun `422 bisnis tanpa errors memakai message`() = runTest {
        val body = """{"message":"Perubahan status tidak valid dari status saat ini"}"""
        val result = safeApiCall<String>(gson) { Response.error(422, errorBody(body)) }
        assertEquals(
            "Perubahan status tidak valid dari status saat ini",
            result.exceptionOrNull()?.message,
        )
    }

    @Test
    fun `error body tak terbaca jatuh ke pesan default per-kode`() = runTest {
        val result = safeApiCall<String>(gson) { Response.error(401, errorBody("")) }
        assertEquals("Sesi berakhir, silakan login kembali", result.exceptionOrNull()?.message)
    }

    @Test
    fun `kode tak dikenal memakai pesan generik dengan kode`() = runTest {
        val result = safeApiCall<String>(gson) { Response.error(500, errorBody("")) }
        assertEquals("Terjadi kesalahan (500)", result.exceptionOrNull()?.message)
    }

    @Test
    fun `IOException dipetakan ke pesan koneksi`() = runTest {
        val result = safeApiCall<String>(gson) { throw IOException("no network") }
        val ex = result.exceptionOrNull() as ApiException
        assertEquals("Periksa koneksi internet Anda", ex.message)
        assertNull(ex.code)
    }

    @Test
    fun `exception lain memakai pesannya`() = runTest {
        val result = safeApiCall<String>(gson) { throw RuntimeException("aneh") }
        assertEquals("aneh", result.exceptionOrNull()?.message)
    }
}
