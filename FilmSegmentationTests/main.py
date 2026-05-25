import cv2 as cv
import numpy as np
import os
import matplotlib.pyplot as plt

from rect_seg import detect_film_frame, extract_and_perspective_correct

img = cv.imread("a.jpg")

img = cv.cvtColor(img, cv.COLOR_BGR2GRAY)
img = ~img 

detected, corners = detect_film_frame(img, show_steps=True)

cv.namedWindow('detected', cv.WINDOW_NORMAL)
cv.resizeWindow('detected', 800, 600)
cv.imshow("detected", detected)

cv.waitKey(0)
cv.destroyAllWindows()

# Jeśli chcesz skorygować perspektywę:
if corners is not None:
    corrected = extract_and_perspective_correct(~img, corners)  # Odwróć z powrotem dla korekcji
    if corrected is not None:
        cv.imshow("Corrected", corrected)
        cv.waitKey(0)
        cv.destroyAllWindows()