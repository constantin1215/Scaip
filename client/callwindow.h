#ifndef CALLWINDOW_H
#define CALLWINDOW_H

#include "VideoMemberWidget.h"
#include "qjsonobject.h"
#include "qvideoframe.h"
#include <AudioWSClient.h>
#include <QAudioSink>
#include <QAudioSource>
#include <QDialog>
#include <QJsonArray>
#include <QMediaCaptureSession>
#include <QMediaPlayer>
#include <QVideoWidget>
#include <VideoWSClient.h>

namespace Ui {
class CallWindow;
}

class CallWindow : public QDialog
{
    Q_OBJECT

public:
    explicit CallWindow(QWidget *parent = nullptr, QJsonObject* eventData = nullptr);
    ~CallWindow();

public Q_SLOTS:
    void onNewVideoWidget(QString username);
    void onNewVideoWidgets(QJsonArray members);
    void onRemovingVideoWidget(QString username);
    void onUpdateFrame(QString userId, QByteArray frameData);
    void onUpdateAudio(QString userId, QByteArray audioData);

private slots:
    void on_toggleVideoButton_clicked();
    void on_leaveCallButton_clicked();
    void on_audioToggleButton_clicked();

    void processFrame(const QVideoFrame &frame);
    void handleAudioState(QAudio::State newState);
    void processSamples(QByteArray data);
    void handleInputDeviceChange(int index);

private:
    Ui::CallWindow *ui;
    QMediaCaptureSession *session;
    QMediaPlayer *player;
    QVideoWidget *videoWidget;
    VideoWSClient *videoClient;
    AudioWSClient *audioClient;
    QAudioSource *audioSource;
    QIODevice* inputSource;
    QAudioSink *audioSink;
    QIODevice *audioOutputDevice;
    QMap<QString, VideoMemberWidget*> videoWidgets;
    bool mockMode = false;

    QByteArray userIdBin;

    void initVideoClient(QString channel);
    void initCamera();
    void initAudioClient(QString channel);
    void checkPermissions();
    void initAudioInput(const QAudioDevice &deviceInfo);
    void initAudioOutput();
};

#endif // CALLWINDOW_H
