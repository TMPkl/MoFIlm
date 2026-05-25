import cv2 
import numpy as np
def detect_film_frame(img, show_steps=False):

    """
    Wykrywa prostokąt klatki filmowej na skanie taśmy 35mm
    
    Args:
        image_path: ścieżka do obrazu
        show_steps: czy pokazywać etapy przetwarzania
    
    Returns:
        detected_frame: obraz z zaznaczonym prostokątem
        corners: współrzędne narożników prostokąta
    """
    
    # Wczytaj obraz
    img = img.copy()
    if img is None:
        raise ValueError(f"Nie można wczytać obrazu z podanej ścieżki.")
    
    original = img.copy()
    
    # Konwersja do skali szarości (jeśli obraz jest kolorowy)
    if len(img.shape) == 3:
        gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    else:
        gray = img.copy()

    
    # Wykrywanie krawędzi metodą Canny
    edges = cv2.Canny(gray, 50, 150, apertureSize=5)
    
    # Operacje morfologiczne do zamknięcia przerw
    kernel = np.ones((3, 3), np.uint8)
    edges = cv2.dilate(edges, kernel, iterations=1)
    edges = cv2.erode(edges, kernel, iterations=1)
    
    # Znajdowanie konturów
    contours, _ = cv2.findContours(edges, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    
    # Sortowanie konturów według powierzchni (malejąco)
    contours = sorted(contours, key=cv2.contourArea, reverse=True)
    
    frame_contour = None
    corners = None
    
    # Szukanie prostokątnej klatki
    for contour in contours[:10]:  # Sprawdź 10 największych konturów
        # Aproksymacja konturu wielokątem
        epsilon = 0.02 * cv2.arcLength(contour, True)
        approx = cv2.approxPolyDP(contour, epsilon, True)
        
        # Sprawdź czy to prostokąt (4 wierzchołki)
        if len(approx) == 4:
            area = cv2.contourArea(approx)
            img_area = img.shape[0] * img.shape[1]
            
            # Sprawdź czy powierzchnia jest odpowiednia (10-80% obrazu)
            if 0.1 * img_area < area < 0.8 * img_area:
                # Sprawdź proporcje (klatka 35mm to ~1.5:1)
                x, y, w, h = cv2.boundingRect(approx)
                aspect_ratio = float(w) / h
                
                # Dla poziomej klatki sprawdź czy proporcje są ok
                if 1.2 < aspect_ratio < 1.8 or 0.55 < aspect_ratio < 0.85:
                    frame_contour = approx
                    corners = approx.reshape(4, 2)
                    break
    
    result = original.copy()
    
    # Jeśli obraz jest w skali szarości, przekonwertuj do BGR dla kolorowych konturów
    if len(result.shape) == 2:
        result = cv2.cvtColor(result, cv2.COLOR_GRAY2BGR)
    
    if frame_contour is not None:
        # Rysuj wykryty prostokąt
        cv2.drawContours(result, [frame_contour], -1, (0, 255, 0), 3)
        
        # Zaznacz narożniki
        for corner in corners:
            cv2.circle(result, tuple(corner), 8, (255, 0, 0), -1)
        
        print("✓ Wykryto klatkę filmową!")
        print(f"Narożniki: {corners}")
        
    else:
        print("✗ Nie znaleziono klatki filmowej")
        corners = None
    
    if show_steps:
        # Pokaż etapy przetwarzania
        cv2.imshow('Original', cv2.resize(original, (800, 600)))
        cv2.imshow('Edges', cv2.resize(edges, (800, 600)))
        cv2.imshow('Result', cv2.resize(result, (800, 600)))
        cv2.waitKey(0)
        cv2.destroyAllWindows()
    
    return result, corners

def extract_and_perspective_correct(img, corners):
    """
    Wyodrębnia i koryguje perspektywę klatki filmowej
    
    Args:
        img: obraz
        corners: współrzędne 4 narożników
    
    Returns:
        corrected: skorygowany obraz klatki
    """
    img = img.copy()
    
    if corners is None:
        return None
    
    # Sortuj narożniki: góra-lewo, góra-prawo, dół-prawo, dół-lewo
    corners = corners.astype(np.float32)
    sum_coords = corners.sum(axis=1)
    diff_coords = np.diff(corners, axis=1)
    
    top_left = corners[np.argmin(sum_coords)]
    bottom_right = corners[np.argmax(sum_coords)]
    top_right = corners[np.argmin(diff_coords)]
    bottom_left = corners[np.argmax(diff_coords)]
    
    src_points = np.array([top_left, top_right, bottom_right, bottom_left], dtype=np.float32)
    
    # Wymiary docelowe (standardowa proporcja 35mm: 36x24mm = 3:2)
    width = 1200
    height = 800
    
    dst_points = np.array([
        [0, 0],
        [width - 1, 0],
        [width - 1, height - 1],
        [0, height - 1]
    ], dtype=np.float32)
    
    # Oblicz macierz transformacji perspektywy
    M = cv2.getPerspectiveTransform(src_points, dst_points)
    
    # Zastosuj transformację
    corrected = cv2.warpPerspective(img, M, (width, height))
    
    return corrected