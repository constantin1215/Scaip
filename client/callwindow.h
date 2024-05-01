#ifndef CALLWINDOW_H
#define CALLWINDOW_H

#include "qjsonobject.h"
#include "qvideoframe.h"
#include <QDialog>
#include <QMediaCaptureSession>
#include <QMediaPlayer>
#include <QVideoWidget>
#include <callwsclient.h>

namespace Ui {
class CallWindow;
}

class CallWindow : public QDialog
{
    Q_OBJECT

public:
    explicit CallWindow(QWidget *parent = nullptr, QJsonObject* eventData = nullptr);
    ~CallWindow();

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
    CallWSClient *client;
};

#endif // CALLWINDOW_H
