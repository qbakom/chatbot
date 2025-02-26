#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <curl/curl.h>
#include <cjson/cJSON.h>

#define BUF_SIZE 8192
#define HISTORY_SIZE 16384

static size_t write_callback(void* contents, size_t size, size_t nmemb, void* userp) {
    size_t total_size = size * nmemb;
    strncat((char*)userp, (char*)contents, total_size);
    return total_size;
}

char* send_chat_request(const char* url, const char* json_payload) {
    static char response[BUF_SIZE];
    memset(response, 0, BUF_SIZE);
    CURL* curl = curl_easy_init();
    if (curl) {
        struct curl_slist* headers = NULL;
        headers = curl_slist_append(headers, "Content-Type: application/json");
        curl_easy_setopt(curl, CURLOPT_URL, url);
        curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headers);
        curl_easy_setopt(curl, CURLOPT_POSTFIELDS, json_payload);
        curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, write_callback);
        curl_easy_setopt(curl, CURLOPT_WRITEDATA, response);
        curl_easy_perform(curl);
        curl_slist_free_all(headers);
        curl_easy_cleanup(curl);
    }
    return response;
}

char* extract_message(const char* raw_response) {
    cJSON* root = cJSON_Parse(raw_response);
    if (!root) return NULL;
    cJSON* choices = cJSON_GetObjectItemCaseSensitive(root, "choices");
    if (!cJSON_IsArray(choices)) {
        cJSON_Delete(root);
        return NULL;
    }
    cJSON* first_choice = cJSON_GetArrayItem(choices, 0);
    if (!first_choice) {
        cJSON_Delete(root);
        return NULL;
    }
    cJSON* message = cJSON_GetObjectItemCaseSensitive(first_choice, "message");
    if (!cJSON_IsObject(message)) {
        cJSON_Delete(root);
        return NULL;
    }
    cJSON* content = cJSON_GetObjectItemCaseSensitive(message, "content");
    if (!cJSON_IsString(content)) {
        cJSON_Delete(root);
        return NULL;
    }
    char* result = strdup(content->valuestring);
    cJSON_Delete(root);
    return result;
}

int starts_with(const char* str, const char* prefix) {
    return strncmp(str, prefix, strlen(prefix)) == 0;
}

int main() {
    static char history[HISTORY_SIZE];
    memset(history, 0, HISTORY_SIZE);
    while (1) {
        char input[BUF_SIZE];
        memset(input, 0, BUF_SIZE);
        printf("> ");
        if (!fgets(input, BUF_SIZE, stdin)) break;
        input[strcspn(input, "\n")] = 0;
        if (starts_with(input, "/exit")) break;
        strncat(history, "{\"role\":\"user\",\"content\":\"", HISTORY_SIZE - strlen(history) - 1);
        strncat(history, input, HISTORY_SIZE - strlen(history) - 1);
        strncat(history, "\"},", HISTORY_SIZE - strlen(history) - 1);
        char payload[HISTORY_SIZE + 512];
        memset(payload, 0, sizeof(payload));
        snprintf(payload, sizeof(payload),
            "{"
            "\"model\":\"deepsseek-r1-distill-qwen-7b\","
            "\"messages\":[{\"role\":\"system\",\"content\":\"You are a helpful assistant.\"},%s{\"role\":\"assistant\",\"content\":\"\"}],"
            "\"max_tokens\":128,"
            "\"temperature\":0.7"
            "}",
            history
        );
        char* raw_response = send_chat_request("http://172.30.160.1:1234/v1/chat/completions", payload);
        char* answer = extract_message(raw_response);
        if (answer) {
            printf("%s\n", answer);
            strncat(history, "{\"role\":\"assistant\",\"content\":\"", HISTORY_SIZE - strlen(history) - 1);
            strncat(history, answer, HISTORY_SIZE - strlen(history) - 1);
            strncat(history, "\"},", HISTORY_SIZE - strlen(history) - 1);
            free(answer);
        }
    }
    return 0;
}
