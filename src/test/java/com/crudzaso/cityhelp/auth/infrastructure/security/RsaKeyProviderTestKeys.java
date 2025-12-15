package com.crudzaso.cityhelp.auth.infrastructure.security;

/**
 * Test-only RSA key material (non-production) reused across unit and integration tests.
 */
public final class RsaKeyProviderTestKeys {
    private RsaKeyProviderTestKeys() {}

    public static final String PRIVATE_KEY = """
            -----BEGIN PRIVATE KEY-----
            MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCxM9WfATdlBN+j
            hAuqJmEgmSJnlr7FIxzTeI9SE0X7QK8g8kxm56noNrss9AWs792UL62YFGauhF3f
            90BXrQ0mdTn3NuKpzgLqpHZF/04dcHe2/b0WWv2VoTbhY/oYsP5GxY6pIX6PQ5ww
            LiQoFlolFjTIuLpbaaDCrN7GQBUS09Y5UJ/yg6aa3b8gCwRoat4BfJaXRbqrBq4G
            7SZBATiZ48/GwBKCj6XEuce+tk/tSRfmJDYMJv3uNp2iz0D3ymPiQbKVza4EqL9k
            sa9yWNrppTEMHLO3raUW2cjdBYrrtoTk0ufHJv2fZ5LiRUv79xVBWaGFeR0FyUep
            l9UnlcjLAgMBAAECggEAAaYqJHiS7UyknI8NVFaKGQ8QkmbSCXltR1lVk1DczSmw
            PfEmg98MP9eujNlyg6yYL8zBY7gHSQCwJo+6/txJHvGaI5i63RPiB0P9Jv0CwZeU
            pzsCSC1HM3eUg3fcnnk0zJZorV52uo38nwT26JNlefd61nldoDcoVb3dG63OysxY
            6GqfJUEn+GEk6QMhURaUaWcPuQlKVIcj7XCghpn7dshpNMX0e1OjVS5v0LWYZbzs
            8YZNfLgcRivdgCqSxO7V7YPXLU+EZArjjcbja0fPE96PxNiSm/xpF5XkBKJkSdUl
            y7fPsi/wI1QdwpJVVk0NJtntNNJlopupKYEU2YlX0QKBgQDn2ENaM2Z7Ho9fL5lA
            QfqU2skwF/vWwJtLiQjr3AA3ewg+JGlSxIHY6cXOizaL9nB3ZbZ7W4GzLhBwDVGA
            9MbAqUPXoYfwN8FHgw4waTDxtUJFc1id+z0REpSfE3hyAjHa/W56U9Guai6ZQxIq
            klrGo/FJeZtF1Iy5X1kIZuGm+wKBgQDDqiiHkmkssBicO3omHCgMYU7R3i/BJ0tV
            0uh7W8QoC49j+RLakk8AxOMvaY6p16ZZi11P+5IHWQtTjqkNaMygQ19dTeO7NwXx
            aSfCH5mtpwsNbLe4p6d9T3P2fAVX8nGNtjIYPmO85nTVvTbHUlycOU73p0uUMxXI
            xk1tFhT8cQKBgHEs6PQHU2eWq3ZTzgPIMYKoqr2Hd7CFpUliQ7CfXJu4u3VaYO++
            bYRt2dnRq3b3fj9Kr6HYMaA+RFfuqHLDCqLR/gQtHibtkLRQDYUkVHgpe4hbnk7A
            bjb/Uv20i39cAupb/KpKcHkr7EN0slGF0DpXnVm4u0uDTo7PNK8dSdoxAoGBAJAW
            DRSQ37yVzPm33uKTl3ZObiGkEdyWWCbSUnsM/61Rnf1L3PNyDJWPZ5FxlSpDcO+9
            7jBJHKcmjm2wv5LtWsql8mMeSZaDFXBKNIoChUPYoA35wm1LoM5ppQadG75A/hgS
            VF+ACiiAOQdw+Bbd8u7kUN+UpdZzrBYW2ct8GGYxAoGBAJIaXv0OSXGuMqUUyNSX
            tXIvyJZSr+tWek8hUJaVPZDCVxI71+NiLvh/s50P9EHhlEZUzrwOubUJ8D3g1N8h
            RisagnboVKkJc+5wfrVTq22ci6+QsH/bO6ZOGEWoNFVtv3kzjqvRy5hRKF24iZBK
            pEEPj78sBd3hh4M3+YUp7+/j
            -----END PRIVATE KEY-----
            """;

    public static final String PRIVATE_KEY_PKCS1 = """
            -----BEGIN RSA PRIVATE KEY-----
            MIIEpAIBAAKCAQEAsTPVnwE3ZQTfo4QLqiZhIJkiZ5a+xSMc03iPUhNF+0CvIPJM
            Zuep6Da7LPQFrO/dlC+tmBRmroRd3/dAV60NJnU59zbiqc4C6qR2Rf9OHXB3tv29
            Flr9laE24WP6GLD+RsWOqSF+j0OcMC4kKBZaJRY0yLi6W2mgwqzexkAVEtPWOVCf
            8oOmmt2/IAsEaGreAXyWl0W6qwauBu0mQQE4mePPxsASgo+lxLnHvrZP7UkX5iQ2
            DCb97jados9A98pj4kGylc2uBKi/ZLGvclja6aUxDByzt62lFtnI3QWK67aE5NLn
            xyb9n2eS4kVL+/cVQVmhhXkdBclHqZfVJ5XIywIDAQABAoIBAAGmKiR4ku1MpJyP
            DVRWihkPEJJm0gl5bUdZVZNQ3M0psD3xJoPfDD/XrozZcoOsmC/MwWO4B0kAsCaP
            uv7cSR7xmiOYut0T4gdD/Sb9AsGXlKc7AkgtRzN3lIN33J55NMyWaK1edrqN/J8E
            9uiTZXn3etZ5XaA3KFW93RutzsrMWOhqnyVBJ/hhJOkDIVEWlGlnD7kJSlSHI+1w
            oIaZ+3bIaTTF9HtTo1Uub9C1mGW87PGGTXy4HEYr3YAqksTu1e2D1y1PhGQK443G
            42tHzxPej8TYkpv8aReV5ASiZEnVJcu3z7Iv8CNUHcKSVVZNDSbZ7TTSZaKbqSmB
            FNmJV9ECgYEA59hDWjNmex6PXy+ZQEH6lNrJMBf71sCbS4kI69wAN3sIPiRpUsSB
            2OnFzos2i/Zwd2W2e1uBsy4QcA1RgPTGwKlD16GH8DfBR4MOMGkw8bVCRXNYnfs9
            ERKUnxN4cgIx2v1uelPRrmoumUMSKpJaxqPxSXmbRdSMuV9ZCGbhpvsCgYEAw6oo
            h5JpLLAYnDt6JhwoDGFO0d4vwSdLVdLoe1vEKAuPY/kS2pJPAMTjL2mOqdemWYtd
            T/uSB1kLU46pDWjMoENfXU3juzcF8Wknwh+ZracLDWy3uKenfU9z9nwFV/JxjbYy
            GD5jvOZ01b02x1JcnDlO96dLlDMVyMZNbRYU/HECgYBxLOj0B1Nnlqt2U84DyDGC
            qKq9h3ewhaVJYkOwn1ybuLt1WmDvvm2EbdnZ0at2934/Sq+h2DGgPkRX7qhywwqi
            0f4ELR4m7ZC0UA2FJFR4KXuIW55OwG42/1L9tIt/XALqW/yqSnB5K+xDdLJRhdA6
            V51ZuLtLg06OzzSvHUnaMQKBgQCQFg0UkN+8lcz5t97ik5d2Tm4hpBHcllgm0lJ7
            DP+tUZ39S9zzcgyVj2eRcZUqQ3Dvve4wSRynJo5tsL+S7VrKpfJjHkmWgxVwSjSK
            AoVD2KAN+cJtS6DOaaUGnRu+QP4YElRfgAoogDkHcPgW3fLu5FDflKXWc6wWFtnL
            fBhmMQKBgQCSGl79DklxrjKlFMjUl7VyL8iWUq/rVnpPIVCWlT2QwlcSO9fjYi74
            f7OdD/RB4ZRGVM68Drm1CfA94NTfIUYrGoJ26FSpCXPucH61U6ttnIuvkLB/2zum
            ThhFqDRVbb95M46r0cuYUShduImQSqRBD4+/LAXd4YeDN/mFKe/v4w==
            -----END RSA PRIVATE KEY-----
            """;

    public static final String PUBLIC_KEY = """
            -----BEGIN PUBLIC KEY-----
            MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAsTPVnwE3ZQTfo4QLqiZh
            IJkiZ5a+xSMc03iPUhNF+0CvIPJMZuep6Da7LPQFrO/dlC+tmBRmroRd3/dAV60N
            JnU59zbiqc4C6qR2Rf9OHXB3tv29Flr9laE24WP6GLD+RsWOqSF+j0OcMC4kKBZa
            JRY0yLi6W2mgwqzexkAVEtPWOVCf8oOmmt2/IAsEaGreAXyWl0W6qwauBu0mQQE4
            mePPxsASgo+lxLnHvrZP7UkX5iQ2DCb97jados9A98pj4kGylc2uBKi/ZLGvclja
            6aUxDByzt62lFtnI3QWK67aE5NLnxyb9n2eS4kVL+/cVQVmhhXkdBclHqZfVJ5XI
            ywIDAQAB
            -----END PUBLIC KEY-----
            """;
}
