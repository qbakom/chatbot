#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <curl/curl.h>
#include "cjson/cJSON.h"

struct Memory {
    char *response;
    size_t size;
};

static size_t WriteCallback(void *data, size_t size, size_t nmemb, void *userp) {
    size_t realsize = size * nmemb;
    struct Memory *mem = (struct Memory *)userp;
    char *ptr = realloc(mem->response, mem->size + realsize + 1);
    if(ptr == NULL) return 0;
    mem->response = ptr;
    memcpy(&(mem->response[mem->size]), data, realsize);
    mem->size += realsize;
    mem->response[mem->size] = 0;
    return realsize;
}

int main() {
    CURL *curl;
    CURLcode res;

    curl_global_init(CURL_GLOBAL_DEFAULT);
    curl = curl_easy_init();
    if(curl) {
        struct Memory chunk;
        chunk.response = malloc(1);
        chunk.size = 0;

        curl_easy_setopt(curl, CURLOPT_URL, "http://localhost:8000/api/chat");

        struct curl_slist *headers = NULL;
        headers = curl_slist_append(headers, "Content-Type: application/json");
        curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headers);

        cJSON *root = cJSON_CreateObject();
        cJSON_AddStringToObject(root, "prompt", "Hello, LM Studio!");

        char *json_string = cJSON_Print(root);
        curl_easy_setopt(curl, CURLOPT_POSTFIELDS, json_string);

        curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, WriteCallback);
        curl_easy_setopt(curl, CURLOPT_WRITEDATA, (void *)&chunk);

        res = curl_easy_perform(curl);
        if(res != CURLE_OK) {
            fprintf(stderr, "Error curl_easy_perform(): %s\n", curl_easy_strerror(res));
        } else {
            printf("Response: %s\n", chunk.response);
        }

        free(chunk.response);
        cJSON_Delete(root);
        free(json_string);
        curl_slist_free_all(headers);
        curl_easy_cleanup(curl);
    }
    curl_global_cleanup();
    return 0;
}
