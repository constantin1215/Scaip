#include "callwindow.h"
#include "ui_callwindow.h"

#include <QAudioDevice>
#include <QAudioInput>
#include <QAudioOutput>
#include <QBuffer>
#include <QCamera>
#include <QCameraDevice>
#include <QIODevice>
#include <QMediaDevices>
#include <QMediaRecorder>
#include <QVideoSink>

CallWindow::CallWindow(QWidget *parent, QJsonObject* eventData)
    : QDialog(parent)
    , ui(new Ui::CallWindow)
{
    ui->setupUi(this);

    setWindowFlags(Qt::Window
                   | Qt::WindowMinimizeButtonHint
                   | Qt::WindowMaximizeButtonHint);

    player = new QMediaPlayer(this);
    videoWidget = new QVideoWidget;
    session = new QMediaCaptureSession(this);
    recorder = new QMediaRecorder(this);

    QUrl url;
    url.setScheme("ws");
    url.setHost("localhost");
    url.setPort(8999);

    client = new CallWSClient(url, true, this);

    const QList<QCameraDevice> cameras = QMediaDevices::videoInputs();

    if (cameras.empty())
        qDebug() << "No cameras detected!";
    else {
        QCamera* camera = new QCamera(cameras[0]);
        session->setCamera(camera);

        connect(camera, &QCamera::errorOccurred, this, [this]() {
            qDebug() << "Error occured. Camera might be used by another process";
            session->setCamera(nullptr);
            player->setSource(QUrl("http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"));
            player->setVideoOutput(videoWidget);
            player->setAudioOutput(new QAudioOutput(QMediaDevices::defaultAudioOutput()));
            player->play();
        });
    }

    if (session->camera())
        session->setVideoOutput(videoWidget);

    ui->videoGridLayout->addWidget(videoWidget);

    if (session->camera())
        session->camera()->start();

    const QList<QAudioDevice> audioInputDevices = QMediaDevices::audioInputs();
    qDebug() << "Audio input" << audioInputDevices[0].mode();

    if (audioInputDevices.empty())
        qDebug() << "No audio inputs detected!";
    else {
        QAudioInput* audioInput = new QAudioInput(audioInputDevices[0], this);
        session->setAudioInput(audioInput);
    }

    const QList<QAudioDevice> audioOutputDevices = QMediaDevices::audioOutputs();
    qDebug() << "Audio output" << audioOutputDevices[0].mode();

    if (audioInputDevices.empty())
        qDebug() << "No audio outputs detected!";
    else {
        QAudioOutput* audioOutput = new QAudioOutput(audioOutputDevices[0], this);
        session->setAudioOutput(audioOutput);
    }

    connect(session->videoSink(), &QVideoSink::videoFrameChanged, this, &CallWindow::processFrame);

    // connect(recorder, &QMediaRecorder::errorOccurred, this, [](QMediaRecorder::Error error, const QString &errorString) {
    //     qDebug() << errorString;
    //     qDebug() << error;
    // });

    //session->setRecorder(recorder);
    //recorder->setQuality(QMediaRecorder::HighQuality);
    //recorder->setOutputLocation(QUrl::fromLocalFile("test.mp4"));
    //recorder->record();
}

CallWindow::~CallWindow()
{
    if (session->camera()) {
        session->camera()->stop();
        delete session->camera();
    }
    player->stop();
    delete player;
    delete session;
    delete ui;
}

void CallWindow::on_toggleVideoButton_clicked()
{
    if(session->camera()) {
        if (session->camera()->isActive()) {
            session->camera()->stop();
        }
        else {
            session->camera()->start();
        }
    }
}


void CallWindow::on_leaveCallButton_clicked()
{
    recorder->stop();
    delete this;
}

void CallWindow::processFrame(const QVideoFrame &frame)
{
    QImage image = frame.toImage();
    QByteArray arr;
    QBuffer buffer(&arr);
    buffer.open(QIODevice::WriteOnly);
    image.save(&buffer, "JPG");

    client->sendFrame(buffer.data());
}

