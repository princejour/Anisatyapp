# إعداد مشروع Firebase لتطبيق أنيستي حنان

هذا التطبيق يعتمد بالكامل على خدمات Firebase (Auth, Firestore, Cloud Messaging). لكي يعمل التطبيق بشكل حقيقي في بيئة الإنتاج، يجب عليك اتباع الخطوات التالية:

## 1. إنشاء مشروع Firebase
1. اذهب إلى [Firebase Console](https://console.firebase.google.com/).
2. اضغط على **Create a project** (إنشاء مشروع) واسمه "أنيستي حنان".
3. اتبع الخطوات حتى ينتهي إنشاء المشروع.

## 2. تفعيل الخدمات المطلوبة
من القائمة الجانبية في Firebase:
- **Authentication**: اذهب إلى Build > Authentication ثم اضغط على Get Started. قم بتفعيل **Email/Password**.
- **Firestore Database**: اذهب إلى Build > Firestore Database ثم اضغط على Create database. اختر **Start in production mode**.
- **Cloud Messaging**: اذهب إلى Engage > Messaging (تكون مفعلة تلقائياً لكن ستحتاجها للإشعارات).

## 3. ربط تطبيق الأندرويد
1. في واجهة مشروع Firebase الرئيسية، اضغط على أيقونة **Android** لإضافة تطبيق جديد.
2. في خانة **Android package name**، يجب عليك إدخال `com.aistudio.anisti.hxkqpz`.
3. اضغط على **Register app**.
4. قم بتحميل ملف **`google-services.json`**.
5. **مهم جداً**: أين أضع ملف `google-services.json`؟
   يجب عليك وضع هذا الملف داخل مجلد `app/` في المشروع الخاص بك (يجب أن يستبدل الملف الوهمي الموجود حالياً).
6. بقية خطوات الربط تم إنجازها مسبقاً في ملفات `build.gradle.kts`، يمكنك تخطيها.

## 4. قواعد الحماية (Firestore Rules)
اذهب إلى Firestore Database ثم إلى تبويب **Rules** وضع القواعد التالية:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // المعلمة تستطيع فعل كل شيء
    match /{document=**} {
      allow read, write: if request.auth != null && request.auth.token.email == 'teacher@anisti.com';
    }
    
    // الولي يستطيع قراءة بيانات ابنه والرسائل الخاصة به فقط (بناءً على كود التلميذ)
    match /students/{studentId} {
      // الولي لا يحتاج لقراءة قائمة التلاميذ كلها، فقط يستعمل الاستعلام بكود الولي
      allow read: if true; // يمكن تقييدها لاحقاً
    }
    match /messages/{messageId} {
      allow read: if true; // التطبيق يفلتر الرسائل من جهة العميل بناءً على الكود
    }
  }
}
```
*ملاحظة: هذه قواعد مبسطة لتسهيل التشغيل الأولي.*

## 5. إنشاء حساب المعلمة
1. في Firebase Authentication > Users، اضغط على **Add user**.
2. البريد الإلكتروني: `teacher@anisti.com`
3. كلمة السر: `123456`
*(يمكنك تغيير البريد وكلمة السر، ولكن استخدمها للدخول في التطبيق كمعلمة)*.

## 6. كود إرسال الإشعارات (Firebase Cloud Functions)
لكي تصل الإشعارات للأولياء عند إرسال ملاحظة أو درس أو عدد، يجب نشر دالة سحابية.
اذهب إلى مجلد جديد في حاسوبك وشغل:
`firebase init functions` (تحتاج لتنصيب Node.js و Firebase CLI).
اختر JavaScript، ثم في ملف `index.js` ضع الكود التالي:

```javascript
const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

exports.sendNotificationOnNewMessage = functions.firestore
    .document('messages/{messageId}')
    .onCreate(async (snap, context) => {
        const message = snap.data();
        
        // جلب التلميذ لمعرفة جهازه
        const studentRef = admin.firestore().collection('students').doc(message.studentId);
        const studentDoc = await studentRef.get();
        
        if (!studentDoc.exists) return null;
        
        const studentData = studentDoc.data();
        const parentToken = studentData.parentDeviceId;
        
        if (!parentToken) {
            console.log('No parent device token for student', message.studentId);
            return null;
        }

        let title = 'رسالة جديدة من أنيستي حنان';
        if (message.type === 'note') title = 'ملاحظة جديدة من أنيستي حنان';
        else if (message.type === 'lesson') title = 'درس جديد من أنيستي حنان';
        else if (message.type === 'grade') title = 'عدد جديد من أنيستي حنان';

        const payload = {
            notification: {
                title: title,
                body: message.title || 'اضغط لرؤية التفاصيل'
            },
            token: parentToken
        };

        try {
            const response = await admin.messaging().send(payload);
            console.log('Successfully sent message:', response);
        } catch (error) {
            console.log('Error sending message:', error);
        }
    });
```
قم بنشر الدالة باستخدام: `firebase deploy --only functions`

## 7. تجربة التطبيق خطوة بخطوة
1. افتح التطبيق واختر "دخول المعلمة".
2. سجل الدخول بـ `teacher@anisti.com` و `123456`.
3. ستجد القوائم الوهمية (الخامسة أ و الخامسة ب) مع تلاميذها جاهزة (التطبيق ينشئها آلياً إذا لم تكن موجودة).
4. يمكنك إنشاء قائمة جديدة بالضغط على "إضافة قسم جديد" واختيار المستوى والفوج.
5. ادخل لقائمة "الخامسة أ"، واضغط على أحد التلاميذ لترى تفاصيله وكود الولي.
6. انسخ كود الولي (مثلاً: HANAN-5A-123).
7. افتح التطبيق في هاتف آخر (أو امسح بيانات التطبيق)، واختر "دخول الولي".
8. أدخل الكود المنسوخ.
9. سيفتح لك تطبيق الولي الخاص بذلك التلميذ، وسيستقبل الإشعارات والرسائل.
