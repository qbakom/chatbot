#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <curl/curl.h>
#include <cjson/cJSON.h>

#define BUFFER_SIZE 8192
#define CONVERSATION_SIZE 16384

static size_t write_callback(void* contents, size_t size, size_t nmemb, void* userp) {
    size_t total_size = size * nmemb;
    strncat((char*)userp, (char*)contents, total_size);
    return total_size;
}

char* send_request(const char* url, const char* payload) {
    CURL* curl;
    CURLcode res;
    static char response_buffer[BUFFER_SIZE];
    memset(response_buffer, 0, BUFFER_SIZE);
    curl = curl_easy_init();
    if (curl) {
        struct curl_slist* headers = NULL;
        headers = curl_slist_append(headers, "Content-Type: application/json");
        curl_easy_setopt(curl, CURLOPT_URL, url);
        curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headers);
        curl_easy_setopt(curl, CURLOPT_POSTFIELDS, payload);
        curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, write_callback);
        curl_easy_setopt(curl, CURLOPT_WRITEDATA, (void*)response_buffer);
        res = curl_easy_perform(curl);
        curl_slist_free_all(headers);
        curl_easy_cleanup(curl);
    }
    return response_buffer;
}

char* extract_completion(const char* response) {
    cJSON* json = cJSON_Parse(response);
    if (!json) return NULL;
    cJSON* completion = cJSON_GetObjectItemCaseSensitive(json, "completion");
    if (!cJSON_IsString(completion)) {
        cJSON_Delete(json);
        return NULL;
    }
    char* result = strdup(completion->valuestring);
    cJSON_Delete(json);
    return result;
}

int starts_with(const char* str, const char* prefix) {
    return strncmp(str, prefix, strlen(prefix)) == 0;
}

void handle_extension(const char* input) {
}

int main() {
    static char conversation[CONVERSATION_SIZE];
    memset(conversation, 0, CONVERSATION_SIZE);
    while (1) {
        char input[BUFFER_SIZE];
        memset(input, 0, BUFFER_SIZE);
        printf("> ");
        if (!fgets(input, BUFFER_SIZE, stdin)) break;
        input[strcspn(input, "\n")] = 0;
        if (starts_with(input, "/")) {
            handle_extension(input);
            continue;
        }
        strncat(conversation, "User: ", CONVERSATION_SIZE - strlen(conversation) - 1);
        strncat(conversation, input, CONVERSATION_SIZE - strlen(conversation) - 1);
        strncat(conversation, "\nAssistant: ", CONVERSATION_SIZE - strlen(conversation) - 1);
        char payload[CONVERSATION_SIZE];
        memset(payload, 0, CONVERSATION_SIZE);
        snprintf(payload, CONVERSATION_SIZE, "{\"prompt\":\"%s\",\"max_tokens\":100}", conversation);
        char* raw_response = send_request("http://localhost:11400/api/generate", payload);
        char* completion = extract_completion(raw_response);
        if (completion) {
            printf("%s\n", completion);
            strncat(conversation, completion, CONVERSATION_SIZE - strlen(conversation) - 1);
            free(completion);
        }
        strncat(conversation, "\n", CONVERSATION_SIZE - strlen(conversation) - 1);
    }
    return 0;
}
