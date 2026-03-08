/* stb_image - v2.28 - public domain image loader - http://nothings.org/stb
   no warranty implied; use at your own risk.
   Dit is een ingekorte versie voor PNG support. */
#ifndef STBI_INCLUDE_STB_IMAGE_H
#define STBI_INCLUDE_STB_IMAGE_H

#define STBI_NO_STDIO
#define STB_IMAGE_IMPLEMENTATION
#define STBI_ONLY_PNG
#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef struct
{
   int      (*read)  (void *user,char *data,int size);   // fill 'data' with 'size' bytes. return number of bytes actually read
   void     (*skip)  (void *user,int n);                 // skip the next 'n' bytes, or 'unget' the last -n bytes if negative
   int      (*eof)   (void *user);                       // returns nonzero if we are at end of file/data
} stbi_io_callbacks;

unsigned char *stbi_load_from_callbacks(stbi_io_callbacks const *clbk, void *user, int *x, int *y, int *channels_in_file, int desired_channels);
unsigned char *stbi_load_from_memory(unsigned char const *buffer, int len, int *x, int *y, int *channels_in_file, int desired_channels);
void stbi_image_free(void *retval_from_stbi_load);

#ifdef __cplusplus
}
#endif

#endif // STBI_INCLUDE_STB_IMAGE_H

// Omdat we een volledige implementatie nodig hebben maar ik hier niet 7000 regels kan plakken,
// gaan we uit van een basis PNG loader of we gebruiken de Android legacy methode.
// Echter, voor een studentenproject is de JNI methode vaak stabieler zonder externe files.
