#ifndef VIDEOMEMBERWIDGET_H
#define VIDEOMEMBERWIDGET_H

#include "qboxlayout.h"
#include <QLabel>
#include <QVideoWidget>
#include <QWidget>

namespace Ui {
class VideoMemberWidget;
}

enum class VideoType {
    SELF,
    EXTERN
};

class VideoMemberWidget : public QWidget
{
    Q_OBJECT

public:
    explicit VideoMemberWidget(QWidget *parent = nullptr,
                               QString username = "",
                               QVideoWidget *videoWidget = nullptr,
                               VideoType type = VideoType::SELF);
    ~VideoMemberWidget();
    void updateFrame(QByteArray frameData);
    void updateUsername(QString username);

private:
    Ui::VideoMemberWidget *ui;
    QLabel* videoMock = nullptr;
    QHBoxLayout *horizontalLayout = nullptr;

    QString userId;
};

#endif // VIDEOMEMBERWIDGET_H
