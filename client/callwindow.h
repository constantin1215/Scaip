#ifndef CALLWINDOW_H
#define CALLWINDOW_H

#include "VideoMemberWidget.h"
#include "qjsonobject.h"
#include "qvideoframe.h"
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

private slots:
    void on_toggleVideoButton_clicked();

    void on_leaveCallButton_clicked();
    void processFrame(const QVideoFrame &frame);

private:
    Ui::CallWindow *ui;
    QMediaCaptureSession *session;
    QMediaPlayer *player;
    QVideoWidget *videoWidget;
    QMediaRecorder *recorder;
    VideoWSClient *client;
    QMap<QString, VideoMemberWidget*> videoWidgets;

    QByteArray userIdBin;
};

#endif // CALLWINDOW_H
